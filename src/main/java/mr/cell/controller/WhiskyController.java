package mr.cell.controller;

import java.util.List;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import mr.cell.dao.WhiskyDao;
import mr.cell.domain.Whisky;

public class WhiskyController {
	
	private static final String JSON_TYPE = "application/json; charset=utf-8";
	private static final String CONTENT_TYPE_HEADER = "Content-Type";
	
	private WhiskyDao dao;
	
	public WhiskyController(WhiskyDao dao) {
		this.dao = dao;
	}
	
	public void getAll(RoutingContext routingContext) {
		dao.getAll(ar -> {
			if(ar.failed()) {
				routingContext.response().setStatusCode(500).end();
				return;
			}
			
			List<Whisky> whiskies = ar.result();
			routingContext.response()
				.putHeader(CONTENT_TYPE_HEADER, JSON_TYPE)
				.end(Json.encodePrettily(whiskies));
		});
	}
	
	public void addOne(RoutingContext routingContext) {
		Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(), Whisky.class);
		dao.addOne(whisky, (ar) -> {
			if(ar.failed()) {
				routingContext.response().setStatusCode(500).end();
				return;
			}
			
			Whisky w = ar.result();
			routingContext.response().setStatusCode(201)
				.putHeader(CONTENT_TYPE_HEADER, JSON_TYPE)
				.end(Json.encodePrettily(w));
		});
	}
	
	public void getId(RoutingContext routingContext) {
		String id = routingContext.request().getParam("id");
		if(id == null) {
			routingContext.response().setStatusCode(400).end();
			return;
		}
		
		Integer idAsInteger = Integer.valueOf(id);
		routingContext.put("id", idAsInteger);
		routingContext.next();
	}
	
	public void getOne(RoutingContext routingContext) {
		Integer id = routingContext.get("id");		
		dao.getOne(id, (ar) -> {
			if(ar.failed()) {
				routingContext.response().setStatusCode(404).end();
				return;
			}
			
			Whisky whisky = ar.result();
			routingContext.response()
				.putHeader(CONTENT_TYPE_HEADER, JSON_TYPE)
				.end(Json.encodePrettily(whisky));
		});
	}
	
	public void updateOne(RoutingContext routingContext) {
		Integer id = routingContext.get("id");
		JsonObject jsonObject = routingContext.getBodyAsJson();
		if(jsonObject == null) {
			routingContext.response().setStatusCode(400).end();
			return;
		}
		
		dao.updateOne(id, jsonObject, (ar) -> {
			if(ar.failed()) {
				routingContext.response().setStatusCode(404).end();
				return;
			}
			
			routingContext.response().putHeader(CONTENT_TYPE_HEADER, JSON_TYPE)
				.end(Json.encodePrettily(ar.result()));
		});
	}
	
	public void deleteOne(RoutingContext routingContext) {
		Integer id = routingContext.get("id");
		dao.deleteOne(id, (ar) -> {
			if(ar.failed()) {
				routingContext.response().setStatusCode(404).end();
				return;
			}
			
			routingContext.response().setStatusCode(204).end();
		});
	}

}
