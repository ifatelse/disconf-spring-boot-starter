package com.lethe.disconf.properties;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

public class EnvironmentChangeEvent extends ApplicationEvent {

	private static final long serialVersionUID = -5996204822623083751L;

	private Set<String> keys;

	public EnvironmentChangeEvent(Object source) {
		super(source);
	}

	public EnvironmentChangeEvent(Object source, Set<String> keys) {
		super(source);
		this.keys = keys;
	}

	/**
	 * @return The keys.
	 */
	public Set<String> getKeys() {
		return this.keys;
	}

}