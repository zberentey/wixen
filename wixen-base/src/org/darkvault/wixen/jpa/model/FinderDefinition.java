package org.darkvault.wixen.jpa.model;

public class FinderDefinition {

	public FinderDefinition(String name, String[] columns, long columnBitmask) {
		this(name, columns, columnBitmask, false);
	}

	public FinderDefinition(
		String name, String[] columns, long columnBitmask, boolean localizedColumns) {

		_columnBitmask = columnBitmask;
		_columns = columns;
		_name = name;
		_localizedColumns = localizedColumns;
	}

	public long getColumnBitmask() {
		return _columnBitmask;
	}

	public String[] getColumns() {
		return _columns;
	}

	public String getName() {
		return _name;
	}

	public boolean hasLocalizedColumns() {
		return _localizedColumns;
	}

	public boolean isUnique() {
		return _unique;
	}

	public void setUnique(boolean unique) {
		_unique = unique;
	}

	private long _columnBitmask;
	private String[] _columns;
	private String _name;
	private boolean _localizedColumns;
	private boolean _unique;

}