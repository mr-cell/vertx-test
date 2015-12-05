package mr.cell;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import mr.cell.domain.Whisky;

/**
 * 
 * @author marcel
 *
 */
public class VertxApp extends AbstractVerticle {
	
	private JDBCClient jdbc;
	
	@Override
	public void start(Future<Void> fut) {
		jdbc = JDBCClient.createShared(vertx, config(), "my-whisky-collection");
		
		startBackend((connection) -> 
			bootstrapData(connection, (nothing) -> 
				startWebApp((http) -> 
					completeStartup(http, fut)
				), fut)
			, fut);		
	}
	
	private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
		jdbc.getConnection(ar -> {
			if(ar.failed()) {
				fut.fail(ar.cause());
				return;
			} 
			next.handle(Future.succeededFuture(ar.result()));
		});
	}
	
	private void bootstrapData(AsyncResult<SQLConnection> result, 
			Handler<AsyncResult<Void>> next, Future<Void> fut) {
		if(result.failed()) {
			fut.fail(result.cause());
			return;
		}
		
		SQLConnection conn = result.result();
		conn.execute("CREATE TABLE IF NOT EXISTS whisky (id INTEGER IDENTITY, "
				+ "name VARCHAR(100), origin VARCHAR(100))", ar -> {
			if(ar.failed()) {
				fut.fail(ar.cause());
				conn.close();
				return;
			}
			
			conn.query("SELECT * FROM whisky", select -> {
				if(select.failed()) {
					fut.fail(select.cause());
					conn.close();
					return;
				}
				
				if(select.result().getNumRows() == 0) {
					insert(
							new Whisky("Jim Beam", "USA"),
							conn,
							(v) -> insert(
									new Whisky("Jack Daniels", "Scotland"),
									conn,
									(r) -> {
										next.handle(Future.<Void>succeededFuture());
										conn.close();
									}));
				} else {
					next.handle(Future.<Void>succeededFuture());
					conn.close();
				}
			});
		});		
	}
	
	private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
		int port = config().getInteger("http.port", 8080);
		Router router = Router.router(vertx);
		
		router.route("/").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html")
				.end("<h1>Whisky Collection</h1>");
		});
		
		router.route("/assets/*").handler(StaticHandler.create("assets"));
		
		router.get("/api/whiskies").handler(this::getAll);
		router.route("/api/whiskies*").handler(BodyHandler.create());
		router.post("/api/whiskies").handler(this::addOne);
		router.get("/api/whiskies/:id").handler(this::getOne);
		router.put("/api/whiskies/:id").handler(this::updateOne);
		router.delete("/api/whiskies/:id").handler(this::deleteOne);
		
		vertx
			.createHttpServer()
			.requestHandler(router::accept)
			.listen(port, next::handle);
	}
	
	private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
		if(http.succeeded()) {
			fut.complete();
		} else {
			fut.fail(http.cause());
		}
	}
	
	@Override
	public void stop() {
		jdbc.close();
	}
	
	private void getAll(RoutingContext routingContext) {
		jdbc.getConnection(ar -> {
			SQLConnection conn = ar.result();
			conn.query("SELECT * FROM whisky", result -> {
				try {
					List<Whisky> whiskies = result.result().getRows().stream()
							.map(Whisky::new).collect(Collectors.toList());
					routingContext.response()
						.putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(whiskies));
				} finally {
					conn.close();
				}
			});
		});
	}
	
	private void addOne(RoutingContext routingContext) {
		jdbc.getConnection(ar -> {
			SQLConnection conn = ar.result();
			Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(), Whisky.class);
			insert(whisky, conn, (r) -> {
				try {
					if(r.failed()) {
						routingContext.response().setStatusCode(500).end();
						return;
					}
					
					routingContext.response().setStatusCode(201)
						.putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(r.result()));
				} finally {
					conn.close();
				}				
			});
		});
	}
	
	private void getOne(RoutingContext routingContext) {
		String id = routingContext.request().getParam("id");
		if(id == null) {
			routingContext.response().setStatusCode(400).end();
			return;
		}
		
		Integer idAsInteger = Integer.valueOf(id);
		
		jdbc.getConnection((ar) -> {
			SQLConnection conn = ar.result();
			select(idAsInteger, conn, result -> {
				try {
					if(result.failed()) {
						routingContext.response().setStatusCode(404).end();
						return;
					}
					
					Whisky whisky = result.result();
					routingContext.response()
						.putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(whisky));					
				} finally {
					conn.close();
				}
			});
		});
	}
	
	private void updateOne(RoutingContext routingContext) {
		String id = routingContext.request().getParam("id");
		JsonObject jsonObject = routingContext.getBodyAsJson();
		if(id == null || jsonObject == null) {
			routingContext.response().setStatusCode(400).end();
			return;
		}
		
		Integer idAsInteger = Integer.valueOf(id);
		jdbc.getConnection((ar) -> {
			SQLConnection conn = ar.result();
			update(idAsInteger, jsonObject, conn, (result) -> {
				try {
					if(result.failed()) {
						routingContext.response().setStatusCode(404).end();
						return;
					}
					
					routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(result.result()));
				} finally {
					conn.close();
				}
			});
		});
	}
	
	private void deleteOne(RoutingContext routingContext) {
		String id = routingContext.request().getParam("id");
		if(id == null) {
			routingContext.response().setStatusCode(400).end();
			return;
		}
		
		Integer idAsInteger = Integer.valueOf(id);
		
		jdbc.getConnection((ar) -> {
			SQLConnection conn = ar.result();
			delete(idAsInteger, conn, (result) -> {
				try {
					if(result.failed()) {
						routingContext.response().setStatusCode(404).end();
						return;
					}
					
					routingContext.response().setStatusCode(204).end();
				} finally {
					conn.close();
				}
			});
		});
	}
	
	private void insert(Whisky whisky, SQLConnection conn, Handler<AsyncResult<Whisky>> next) {
		String sql = "INSERT INTO whisky(name, origin) VALUES (?, ?)";
		conn.updateWithParams(sql, 
				new JsonArray().add(whisky.getName()).add(whisky.getOrigin()), 
				insert -> {
					if(insert.failed()) {
						next.handle(Future.failedFuture(insert.cause()));
						return;
					}
					
					UpdateResult result = insert.result();
					Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
					next.handle(Future.succeededFuture(w));
				});
	}
	
	private void select(Integer id, SQLConnection conn, Handler<AsyncResult<Whisky>> next) {
		String sql = "SELECT * from whisky WHERE id = ?";
		conn.queryWithParams(sql, new JsonArray().add(id), (select) -> {
			if(select.failed()) {
				next.handle(Future.failedFuture(select.cause()));
				return;
			} else if(select.result().getNumRows() < 1) {
				next.handle(Future.failedFuture("Whisky not found"));
				return;
			}
			
			next.handle(Future.succeededFuture(new Whisky(select.result().getRows().get(0))));			
		});
	}
	
	private void update(Integer id, JsonObject content, SQLConnection conn, Handler<AsyncResult<Whisky>> next) {
		String sql = "UPDATE whisky SET name = ?, origin = ? WHERE id = ?";
		conn.updateWithParams(sql, 
				new JsonArray().add(content.getString("name")).add(content.getString("origin")).add(id), 
				update -> {
					if(update.failed()) {
						next.handle(Future.failedFuture(update.cause()));
						return;
					} else if(update.result().getUpdated() < 1) {
						next.handle(Future.failedFuture("Whisky not found"));
						return;
					}
					
					next.handle(Future.succeededFuture(new Whisky(Integer.valueOf(id),
							content.getString("name"), content.getString("origin"))));
				});
	}
	
	private void delete(Integer id, SQLConnection conn, Handler<AsyncResult<Void>> next) {
		String sql = "DELETE FROM whisky WHERE id = ?";
		conn.updateWithParams(sql, new JsonArray().add(id), (delete) -> {
			if(delete.failed()) {
				next.handle(Future.failedFuture(delete.cause()));
				return;
			} else if(delete.result().getUpdated() < 1) {
				next.handle(Future.failedFuture("Whisky not found"));
				return;
			}
			
			next.handle(Future.<Void>succeededFuture());
		});
	}
}
