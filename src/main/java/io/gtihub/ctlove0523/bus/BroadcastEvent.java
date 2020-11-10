package io.gtihub.ctlove0523.bus;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BroadcastEvent implements Serializable {
	/**
	 * 0：事件；1：事件确认消息
	 */
	private int type;
	private String id;
	private Map<String, Object> headers;
	private String body;
	private Class<?> bodyClass;
}
