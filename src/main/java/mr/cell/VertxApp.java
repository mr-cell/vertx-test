package mr.cell;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import mr.cell.controller.WhiskyController;
import mr.cell.dao.WhiskyDao;

/**
 * 
 * @author marcel
 *
 */
public class VertxApp extends AbstractVerticle {
	
	private JDBCClient jdbc;
	private WhiskyController controller;
	
	@Override
	public void start(Future<Void> fut) {
		vertx.<JDBCClient>executeBlocking(future -> {
			JDBCClient jdbc = JDBCClient.createShared(vertx, config(), "my-whisky-collection");
			future.complete(jdbc);
		}, res -> {
			this.jdbc = res.result();
			Bootstrap bootstrap = new Bootstrap(jdbc);
			WhiskyDao dao = new WhiskyDao(jdbc);
			controller = new WhiskyController(dao);
			
			bootstrap.initDatabase((nothing) -> 
				startWebApp((http) -> 
					completeStartup(http, fut)
				), fut);
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
		
		router.get("/api/whiskies").handler(controller::getAll);
		router.route("/api/whiskies*").handler(BodyHandler.create());
		router.post("/api/whiskies").handler(controller::addOne);
		
		router.get("/api/whiskies/:id").handler(controller::getId);
		router.get("/api/whiskies/:id").handler(controller::getOne);
		
		router.put("/api/whiskies/:id").handler(controller::getId);
		router.put("/api/whiskies/:id").handler(controller::updateOne);
		
		router.delete("/api/whiskies/:id").handler(controller::getId);
		router.delete("/api/whiskies/:id").handler(controller::deleteOne);
		
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
}
