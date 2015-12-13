package mr.cell.dao;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import mr.cell.domain.Whisky;

public class WhiskyDao {
	
	private final JDBCClient jdbc;
	
	public WhiskyDao(JDBCClient jdbc) {
		this.jdbc = jdbc;
	}
	
	public void getAll(Handler<AsyncResult<List<Whisky>>> handler) {
		jdbc.getConnection(ar -> {
			SQLConnection conn = ar.result();
			conn.query("SELECT * FROM whisky", result -> {
				try {
					if(result.failed()) {
						handler.handle(Future.failedFuture(result.cause()));
						return;
					}
					List<Whisky> whiskies = result.result().getRows().stream()
							.map(Whisky::new).collect(Collectors.toList());
					handler.handle(Future.succeededFuture(whiskies));
				} finally {
					conn.close();
				}
			});
		});
	}
	
	public void addOne(Whisky whisky, Handler<AsyncResult<Whisky>> handler) {
		jdbc.getConnection(ar -> {
			SQLConnection conn = ar.result();
			insert(whisky, conn, (r) -> {
				try {
					if(r.failed()) {
						handler.handle(Future.failedFuture(r.cause()));
						return;
					}
					
					handler.handle(Future.succeededFuture(r.result()));
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
	
	public void getOne(Integer id, Handler<AsyncResult<Whisky>> handler) {
		jdbc.getConnection((ar) -> {
			SQLConnection conn = ar.result();
			select(id, conn, result -> {
				try {
					if(result.failed()) {
						handler.handle(Future.failedFuture(result.cause()));
						return;
					}
					
					Whisky whisky = result.result();
					handler.handle(Future.succeededFuture(whisky));					
				} finally {
					conn.close();
				}
			});
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
	
	public void updateOne(Integer id, JsonObject content, Handler<AsyncResult<Whisky>> handler) {
		jdbc.getConnection((ar) -> {
			SQLConnection conn = ar.result();
			update(id, content, conn, (result) -> {
				try {
					if(result.failed()) {
						handler.handle(Future.failedFuture(result.cause()));
						return;
					}
					
					handler.handle(Future.succeededFuture(result.result()));
				} finally {
					conn.close();
				}
			});
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
	
	public void deleteOne(Integer id, Handler<AsyncResult<Void>> handler) {
		jdbc.getConnection((ar) -> {
			SQLConnection conn = ar.result();
			delete(id, conn, (result) -> {
				try {
					if(result.failed()) {
						handler.handle(Future.failedFuture(result.cause()));
						return;
					}
					
					handler.handle(Future.succeededFuture(result.result()));
				} finally {
					conn.close();
				}
			});
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
