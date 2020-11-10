package io.gtihub.ctlove0523.bus;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BroadcastEvent implements Serializable {
	private String id;
	private Map<String, Object> headers;
	private String body;
	private Class<?> bodyClass;
}
