package com.mycompany.app;

/** What happens to referencing (child) rows when the referenced (parent) row is deleted. */
public enum ReferentialAction {
  /** Reject the delete if any child row still references the parent. This is the default. */
  NO_ACTION("NO ACTION"),
  /** Reject the delete if any child row still references the parent. */
  RESTRICT("RESTRICT"),
  /** Delete the child rows as well. */
  CASCADE("CASCADE"),
  /** Set the referencing columns of the child rows to NULL. */
  SET_NULL("SET NULL"),
  /** Set the referencing columns of the child rows to their column defaults. */
  SET_DEFAULT("SET DEFAULT");

  private final String sql;

  ReferentialAction(String sql) {
    this.sql = sql;
  }

  /**
   * Whether this action blocks deleting a parent row that is still referenced.
   *
   * @return true for NO ACTION and RESTRICT
   */
  public boolean blocks() {
    return this == NO_ACTION || this == RESTRICT;
  }

  /**
   * The action as written in SQL.
   *
   * @return e.g. {@code SET NULL}
   */
  @Override
  public String toString() {
    return sql;
  }
}
