/*
 * The contents of this file are subject to the Interbase Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy
 * of the License at http://www.Inprise.com/IPL.html
 *
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code was created by Inprise Corporation
 * and its predecessors. Portions created by Inprise Corporation are
 * Copyright (C) Inprise Corporation.
 * All Rights Reserved.
 * Contributor(s): ______________________________________.
 */

package interbase.interclient;

/** 
 * @author Paul Ostler
 **/
final class IBTypes
{
  // Warning: These values must match those in interserver/IB_Types.h
  static final int NULL_TYPE__ = 0;
  static final int SMALLINT__ = 1;
  static final int INTEGER__ = 2;
  static final int FLOAT__ = 3;
  static final int DOUBLE__ = 4;
  static final int NUMERIC_DOUBLE__ = 5;
  static final int NUMERIC_INTEGER__ = 6;
  static final int NUMERIC_SMALLINT__ = 7;
  static final int CHAR__ = 8;
  static final int VARCHAR__ = 9;
  static final int CLOB__ = 10;
  static final int DATE__ = 11;
  static final int BLOB__ = 12;
  static final int ARRAY__ = 14;

  static final int SMALLINT_PRECISION__ = 5;
  static final int INTEGER_PRECISION__ = 10;
  static final int FLOAT_PRECISION__ = 7;
  static final int DOUBLE_PRECISION__ = 15;
  static final int NUMERIC_DOUBLE_PRECISION__ = DOUBLE_PRECISION__;
  static final int NUMERIC_INTEGER_PRECISION__ = INTEGER_PRECISION__;
  static final int NUMERIC_SMALLINT_PRECISION__ = SMALLINT_PRECISION__;
  static final int DATE_PRECISION__ = 19;

  static boolean isNumeric (int ibType)
  {
    switch (ibType) {
    case NUMERIC_DOUBLE__:
    case NUMERIC_INTEGER__:
    case NUMERIC_SMALLINT__:
      return true;
    default:
      return false;
    }
  }

  static int getSQLType (int ibType)
  {
    switch (ibType) {
    case NULL_TYPE__: 
      return java.sql.Types.NULL;
    case SMALLINT__: 
      return java.sql.Types.SMALLINT;
    case INTEGER__: 
      return java.sql.Types.INTEGER;
    case FLOAT__: 
      return java.sql.Types.FLOAT;
    case DOUBLE__: 
      return java.sql.Types.DOUBLE;
    case NUMERIC_DOUBLE__: 
    case NUMERIC_INTEGER__: 
    case NUMERIC_SMALLINT__: 
      return java.sql.Types.NUMERIC;
    case CHAR__: 
      return java.sql.Types.CHAR;
    case VARCHAR__: 
      return java.sql.Types.VARCHAR;
    case DATE__: 
      return java.sql.Types.TIMESTAMP;
    case CLOB__: 
      return java.sql.Types.LONGVARCHAR;
    case BLOB__: 
      return java.sql.Types.LONGVARBINARY;
    case ARRAY__: 
      return java.sql.Types.OTHER;
    default:
      return java.sql.Types.NULL;
    }
  }

  static final int TYPE_NAME_PRECISION__ = 16;
 
  static String getIBTypeName (int ibType)
  {
    switch (ibType) {
    case SMALLINT__: 
      return "SMALLINT";
    case INTEGER__: 
      return "INTEGER";
    case FLOAT__: 
      return "FLOAT";
    case DOUBLE__: 
      return "DOUBLE PRECISION";
    case NUMERIC_DOUBLE__: 
    case NUMERIC_INTEGER__: 
    case NUMERIC_SMALLINT__: 
      return "NUMERIC";
    case CHAR__: 
      return "CHAR";
    case VARCHAR__: 
      return "VARCHAR";
    case DATE__: 
      return "DATE";
    case CLOB__: 
    case BLOB__: 
      return "BLOB";
    case ARRAY__: 
      return "ARRAY";
    case NULL_TYPE__: 
      return "";
    default:
      return "";      
    }
  }
}
