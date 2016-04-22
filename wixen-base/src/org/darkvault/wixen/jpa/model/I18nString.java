package org.darkvault.wixen.jpa.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class I18nString implements Serializable {

	public I18nString() {
	}

	public I18nString(String rawValue) {
		set(rawValue);
	}

	public void add(String value, String language) {
		if ((language == null) || (value == null)) {
			return;
		}

		_values.put(language.toLowerCase(), value);
	}

	public String get(String language) {
		if (_values.size() == 0) {
			return null;
		}

		if (language == null) {
			for (String value : _values.values()) {
				return value;
			}
		}

		String s = _values.get(language.toLowerCase());

		if (s == null) {
			s = _values.get(language.toUpperCase());

			if (s == null) {
				for (String value : _values.values()) {
					return value;
				}
			}
		}

		return s;
	}

	public Set<String> getLanguages(){
		return Collections.unmodifiableSet(_values.keySet());
	}

	public boolean isEmpty() {
		return _values.isEmpty();
	}

	public void remove(String language) {
		_values.remove(language.toLowerCase());
	}

	/**
	 * @param rawValue eg. hu{Charter repülőjegy - Egyiptom}en{}ro{Bilet charter - Egipt}
	 */
	public void set(String rawValue) {
		if (rawValue == null) {
			return;
		}

		if (!rawValue.contains("{")) {
			_values.put("__", rawValue);

			return;
		}

		Matcher m = _I18N_PATTERN.matcher(rawValue);

		if (!m.matches()) {
			throw new IllegalArgumentException(
				"Illegal I18nString representation:" + rawValue +
					"Valid language blocks are: '<lang>{<value>}'");
		}

		String[] split = rawValue.split("(\\{|\\})");

		for (int i = 0; i < split.length; i = i + 2) {
			String text = "";

			if (i < (split.length - 1)) {
				text = split[i + 1];
			}

			_values.put(split[i], text);
		}
	}

	@Override
	public String toString() {
		if (_values.isEmpty()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, String> entry : _values.entrySet()) {
			sb.append(entry.getKey());
			sb.append("{");
			sb.append(entry.getValue());
			sb.append("}");
		}

		return sb.toString();
	}

	private static final Pattern _I18N_PATTERN = Pattern.compile("^([a-zA-Z]{2}[{].*?[}])*$");

	private final Map<String, String> _values = new HashMap<>(10);

}