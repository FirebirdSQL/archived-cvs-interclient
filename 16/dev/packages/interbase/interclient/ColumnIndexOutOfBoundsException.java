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
 * An application is referring to a SQL result column index
 * which is out of bounds.
 * <p>
 * The error code associated with this exception is
 * {@link ErrorCodes#invalidOperation ErrorCodes.invalidOperation}.
 *
 * @author Paul Ostler
 * @since <font color=red>Extension, since InterClient 1.0</font> 
 **/
final public class ColumnIndexOutOfBoundsException extends InvalidArgumentException 
{
  final private static String className__ = "ColumnIndexOutOfBoundsException";

  // *** InterClient constructor ****
  ColumnIndexOutOfBoundsException (ErrorKey errorKey, int column) 
  {
    super (className__, errorKey, String.valueOf (column));
  }
}
 
