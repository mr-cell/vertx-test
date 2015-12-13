package mr.cell.domain;

import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class Whisky {
	
	private final Integer id;
	private String name;
	private String origin;
	
	public Whisky() {
		this.id = null;
	}
	
	public Whisky(String name, String origin) {
		this.id = null;
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
