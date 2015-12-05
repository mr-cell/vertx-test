package mr.cell.domain;

import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class Whisky {
	
	private static AtomicInteger COUNTER = new AtomicInteger();
	
	private final Integer id;
	private String name;
	private String origin;
	
	public Whisky() {
		id = COUNTER.getAndIncrement();
	}
	
	public Whisky(String name, String origin) {
		this();
		this.name = name;
		this.origin = origin;
	}
	
	public Whisky(int id, String name, String origin) {
		this.id = id;
		this.name = name;
		this.origin = origin;
	}
	
	public Whisky(JsonObject jsonObject) {
		id = jsonObject.getInteger("ID");
		name = jsonObject.getString("NAME");
		origin = jsonObject.getString("ORIGIN");
	}

}
