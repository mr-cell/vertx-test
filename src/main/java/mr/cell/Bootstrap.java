package mr.cell;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import mr.cell.dao.WhiskyDao;
import mr.cell.domain.Whisky;

public class Bootstrap {

	private WhiskyDao dao;
	private JDBCClient jdbc;
	
	public Bootstrap(JDBCClient jdbc) {
		this.jdbc = jdbc;
		dao = new WhiskyDao(jdbc);
	}
	
	public void initDatabase(Handler<AsyncResult<Void>> next, Future<Void> fut) {
		jdbc.getConnection((connection) -> {
			if(connection.failed()) {
				fut.fail(connection.cause());
				return;
			}
			
			bootstrapData(connection, next, fut);
		});
	}
	
	private void bootstrapData(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, 
			Future<Void> fut) {
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
					dao.addOne(new Whisky("Jim Beam", "USA"), (v) -> {
						dao.addOne(new Whisky("Jack Daniels", "Scotland"), (r) -> {
							next.handle(Future.<Void>succeededFuture());
							conn.close();
						});
					});
				} else {
					next.handle(Future.<Void>succeededFuture());
					conn.close();
				}
			});
		});
	}
}
