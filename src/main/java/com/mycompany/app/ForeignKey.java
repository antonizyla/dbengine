package com.mycompany.app;

import java.io.Serializable;
import java.util.List;

/**
 * A foreign key constraint: the child columns must match the full primary key of a parent row.
 *
 * <p>The child and parent column lists are the same length and paired by position. Once a
 * constraint has been validated by the database, the parent columns are in the order of the
 * parent's primary key, so the child values joined together are exactly the parent row's key.
 *
 * @param name constraint name, unique within the database
 * @param childTable the referencing table
 * @param childColumns the referencing columns
 * @param parentTable the referenced table
 * @param parentColumns the referenced columns, the parent's primary key
 * @param onDelete what to do with child rows when a parent row is deleted
 */
public record ForeignKey(
    String name,
    String childTable,
    List<String> childColumns,
    String parentTable,
    List<String> parentColumns,
    ReferentialAction onDelete)
    implements Serializable {

  /**
   * Create a foreign key, copying the column lists so the constraint cannot change later.
   *
   * @param name constraint name
   * @param childTable the referencing table
   * @param childColumns the referencing columns
   * @param parentTable the referenced table
   * @param parentColumns the referenced columns
   * @param onDelete what to do with child rows when a parent row is deleted
   */
  public ForeignKey {
    childColumns = List.copyOf(childColumns);
    parentColumns = List.copyOf(parentColumns);
    if (onDelete == null) {
      onDelete = ReferentialAction.NO_ACTION;
    }
  }

  /**
   * Human readable form of the referenced columns, e.g. {@code users.id} or {@code orders(id,
   * line)}.
   *
   * @return the referenced table and columns
   */
  public String describeParent() {
    if (parentColumns.size() == 1) {
      return parentTable + "." + parentColumns.get(0);
    }
    return parentTable + "(" + String.join(", ", parentColumns) + ")";
  }
}
