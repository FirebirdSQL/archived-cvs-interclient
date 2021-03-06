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
 * A SQL container used for executing SQL, and a factory for result sets.
 *
 * <p>A Statement object is used for executing a static SQL statement
 * and obtaining the results produced by it.
 *
 * <P>Only one ResultSet per Statement can be open at any point in
 * time. Therefore, if the reading of one ResultSet is interleaved
 * with the reading of another, each must have been generated by
 * different Statements. All statement execute methods implicitly
 * close a statment's current ResultSet if an open one exists.
 *
 * @see Connection#createStatement
 * @see ResultSet
 * @author Paul Ostler
 * @author Madhukar Thakur (escape syntax processing)
 * @since <font color=red>JDBC 1, with extended behavior in JDBC 2</font>
 **/
public class Statement implements java.sql.Statement
{
  RecvMessage prefetchedRecvMsg_ = null;

  // Server-side statement object reference.
  int statementRef_ = 0; // seen by friendly result sets

  JDBCNet jdbcNet_;

  Connection connection_; // private protected

  ResultSet resultSet_ = null; // Seen by friendly connection and child statement classes.

  Integer updateCountStack_ = null; // private protected
  ResultSet resultSetStack_ = null; // Only set on calls to execute

  boolean openOnClient_ = true; // private protected
  boolean openOnServer_ = false; // private protected

  int timeout_ = 0; // private protected
  int maxRows_ = 0; // Seen by friendly result sets
  int maxFieldSize_ = 0; // private protected
  int fetchSize_ = 0;

  java.sql.SQLWarning sqlWarnings_ = null;

  String cursorName_;

  boolean escapeProcessingEnabled_ = true;

  Statement (JDBCNet jdbcNet,
             Connection connection)
  {
    connection_ = connection;
    jdbcNet_ = jdbcNet;
    cursorName_ = ""; //"CRSR" + connection.getNextCursorNameSuffix ();
  }

  void checkForClosedStatement () throws java.sql.SQLException
  {
    if (!openOnClient_)
      throw new InvalidOperationException (ErrorKey.invalidOperation__statement_closed__);
  }

  void checkForEmptySQL (String sql) throws java.sql.SQLException
  {
    if (sql == null || "".equals (sql))
      throw new InvalidArgumentException (ErrorKey.invalidArgument__sql_empty_or_null__);
  }

  // finalizer should return jdbcNet buffers to a pool?

  /**
   * A statement will be closed when its finalizer is called
   * by the garbage collector.  However, there is no guarantee
   * that the garbage collector will ever run, and in general
   * will not run when an application terminates abruptly
   * without closing its resources.
   * <p>
   * Therefore, it is recommended that prepared statements be
   * explicitly closed even if your application throws an exception.
   * This can be achieved by placing a call to close() in a finally
   * clause of your application as follows
   * <pre>
   * try {
   *   ...
   * }
   * finally {
   *   if (statement != null)
   *     try { statement.close (); } catch (SQLException e) {}
   *   if (connection != null)
   *     try { connection.close (); } catch (SQLException e) {}
   * }
   * </pre>
   * <p>
   * Or alternatively, use the System.runFinalizersOnExit () method.
   * @since <font color=red>Extension</font>
   **/
  protected void finalize () throws java.lang.Throwable
  {
    if (openOnServer_)
      close ();

    super.finalize ();
  }

  /**
   * Execute a SQL statement that returns a single ResultSet.
   *
   * @param sql typically this is a static SQL SELECT statement
   * @return a ResultSet that contains the data produced by the query; never null
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public java.sql.ResultSet executeQuery (String sql) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    clearWarnings ();

    if (resultSet_ != null) {
      resultSet_.local_Close ();
      resultSet_ = null;
    }

    updateCountStack_ = null;

    checkForEmptySQL (sql);

    if (escapeProcessingEnabled_) {
      EscapeProcessor escapeProcessor = new EscapeProcessor();
      sql = escapeProcessor.doEscapeProcessing(sql);
    }

    connection_.transactionStartedOnClient_ = true;

    remote_EXECUTE_QUERY_STATEMENT (sql);

    connection_.transactionStartedOnServer_ = true;
    openOnServer_ = true;
    resultSetStack_ = null;

    return resultSet_;
  }

  void remote_EXECUTE_QUERY_STATEMENT (String sql) throws java.sql.SQLException
  {
    MessageBufferOutputStream sendMsg = jdbcNet_.createMessage ();

    sendMsg.writeByte (MessageCodes.EXECUTE_QUERY_STATEMENT__);
    send_StatementExecuteData (sendMsg, sql);

    RecvMessage recvMsg = null;
    try {
      recvMsg = jdbcNet_.sendAndReceiveMessage (sendMsg);

      if (!recvMsg.get_SUCCESS ()) {
	throw recvMsg.get_EXCEPTIONS ();
      }

      // statementRef_ must be set before calling prefetch!
      statementRef_ = recvMsg.readInt ();

      if (!recvMsg.getHeaderEndOfStream ())
	remote_sendPrefetch ();

      int resultCols = recvMsg.readUnsignedShort ();

      if (resultCols == 0) {
        setWarning (recvMsg.get_WARNINGS ()); // !!! Is this really where I should get warnings?
	jdbcNet_.destroyRecvMessage (recvMsg);
      }
      else {
        resultSet_ = new ResultSet (this, jdbcNet_, recvMsg, resultCols, true);
        resultSet_.cursorName_ = cursorName_;
        resultSet_.recv_ResultMetaData (recvMsg);
        resultSet_.numRows_ = recvMsg.readInt ();
        resultSet_.setNumDataPositions (resultCols);
        resultSet_.saveRowPosition ();
        // recvMsg remains associated with resultSet_
      }
    }
    catch (java.sql.SQLException e) {
      if (resultSet_ != null)
        resultSet_.local_Close ();
      resultSet_ = null;
      jdbcNet_.destroyRecvMessage (recvMsg);
      throw e;
    }
  }

  void remote_sendPrefetch () throws java.sql.SQLException
  {
    MessageBufferOutputStream sendMsg = jdbcNet_.createMessage ();
    sendMsg.writeByte (MessageCodes.FETCH_ROWS__);
    sendMsg.writeInt (statementRef_);
    sendMsg.writeInt (fetchSize_);
    if (Globals.debug__) Globals.trace ("fetchSize = " + fetchSize_);
    jdbcNet_.sendPrefetchMessage (this, sendMsg);
  }

  RecvMessage remote_recvPrefetch () throws java.sql.SQLException
  {
    RecvMessage recvMsg = jdbcNet_.receivePrefetchMessage (this);
    if (!recvMsg.get_SUCCESS ())
      throw recvMsg.get_EXCEPTIONS ();
    return recvMsg;
  }

  /**
   * Execute a SQL INSERT, UPDATE or DELETE statement. In addition,
   * SQL statements that return nothing such as SQL DDL statements
   * can be executed.
   *
   * @param sql a SQL INSERT, UPDATE or DELETE statement or a SQL
   *   statement that returns nothing
   * @return either the row count for INSERT, UPDATE or DELETE or 0
   *   for SQL statements that return nothing
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public int executeUpdate (String sql) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    clearWarnings ();

    if (resultSet_ != null) {
      resultSet_.local_Close ();
      resultSet_ = null;
    }

    updateCountStack_ = null;

    checkForEmptySQL (sql);

    if (escapeProcessingEnabled_) {
      EscapeProcessor escapeProcessor = new EscapeProcessor();
      sql = escapeProcessor.doEscapeProcessing(sql);
    }

    connection_.transactionStartedOnClient_ = true;

    int updateCountOrSelectValue = remote_EXECUTE_UPDATE_STATEMENT (sql);

    connection_.transactionStartedOnServer_ = true;
    // Change from openOnServer = false; to openOnServer = true; Bug No. 447462
    openOnServer_ = true;
    resultSetStack_ = null;

    return (updateCountOrSelectValue);
  }

  private int remote_EXECUTE_UPDATE_STATEMENT (String sql) throws java.sql.SQLException
  {
    MessageBufferOutputStream sendMsg = jdbcNet_.createMessage ();

    sendMsg.writeByte (MessageCodes.EXECUTE_UPDATE_STATEMENT__);
    send_StatementExecuteData (sendMsg, sql);

    RecvMessage recvMsg = null;
    try {
      recvMsg = jdbcNet_.sendAndReceiveMessage (sendMsg);

      if (!recvMsg.get_SUCCESS ()) {
	throw recvMsg.get_EXCEPTIONS ();
      }

      statementRef_ = recvMsg.readInt ();
      int updateCountOrSelectValue = recvMsg.readInt ();

      setWarning (recvMsg.get_WARNINGS ());

      return updateCountOrSelectValue;
    }
    catch (java.sql.SQLException e) {
      if (resultSet_ != null)
        resultSet_.local_Close ();
      resultSet_ = null;
      throw e;
    }
    finally {
      jdbcNet_.destroyRecvMessage (recvMsg);
    }
  }

  /**
   * In many cases, it is desirable to immediately release a
   * Statement's database and JDBC resources instead of waiting for
   * this to happen when it is automatically closed; the close
   * method provides this immediate release.
   *
   * <P><B>Note:</B> A Statement is automatically closed when it is
   * garbage collected. When a Statement is closed, its current
   * ResultSet, if one exists, is also closed.
   *
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public void close () throws java.sql.SQLException
  {
    if (!openOnClient_)
      return;

    if (openOnServer_) {
      remote_CLOSE_STATEMENT ();
    }

    local_Close ();
  }

  // Also called by Connection.close(), rollback(), commit() to mark open statements as closed.
  void local_Close () throws java.sql.SQLException
  {
    if (resultSet_ != null)
      resultSet_.local_Close ();
    openOnClient_ = false;
    openOnServer_ = false;
    connection_.openStatements_.removeElement (this);
  }

  void remote_CLOSE_STATEMENT () throws java.sql.SQLException
  {
    MessageBufferOutputStream sendMsg = jdbcNet_.createMessage ();

    sendMsg.writeByte (MessageCodes.CLOSE_STATEMENT__);
    sendMsg.writeInt (statementRef_);

    RecvMessage recvMsg = null;
    try {
      recvMsg = jdbcNet_.sendAndReceiveMessage (sendMsg);

      if (!recvMsg.get_SUCCESS ()) {
	throw recvMsg.get_EXCEPTIONS ();
      }

      setWarning (recvMsg.get_WARNINGS ());
    }
    finally {
      jdbcNet_.destroyRecvMessage (recvMsg);
    }
  }

  /**
   * The maxFieldSize limit (in bytes) is the maximum amount of data
   * returned for any column value; it only applies to BINARY,
   * VARBINARY, LONGVARBINARY, CHAR, VARCHAR, and LONGVARCHAR
   * columns.  If the limit is exceeded, the excess data is silently
   * discarded.
   *
   * @return the current max column size limit; zero means unlimited
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  public int getMaxFieldSize () throws java.sql.SQLException
  {
    return maxFieldSize_;
  }

  /**
   * The maxFieldSize limit (in bytes) is set to limit the size of
   * data that can be returned for any column value; it only applies
   * to BINARY, VARBINARY, LONGVARBINARY, CHAR, VARCHAR, and
   * LONGVARCHAR fields.  If the limit is exceeded, the excess data
   * is silently discarded. For maximum portability use values
   * greater than 256.
   *
   * @param max the new max column size limit; zero means unlimited
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public void setMaxFieldSize (int maxFieldSize) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    maxFieldSize_ = maxFieldSize;
  }

  /**
   * The maxRows limit is the maximum number of rows that a
   * ResultSet can contain.  If the limit is exceeded, the excess
   * rows are silently dropped.
   *
   * @return the current max row limit; zero means unlimited
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  public int getMaxRows () throws java.sql.SQLException
  {
    return maxRows_;
  }

  /**
   * The maxRows limit is set to limit the number of rows that any
   * ResultSet can contain.  If the limit is exceeded, the excess
   * rows are silently dropped.
   *
   * @param max the new max rows limit; zero means unlimited
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public void setMaxRows (int maxRows) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    if (maxRows < 0)
      throw new InvalidArgumentException (ErrorKey.invalidArgument__negative_max_rows__);

    if ((fetchSize_ > 0) && (maxRows < fetchSize_))
      throw new InvalidArgumentException (ErrorKey.invalidArgument__fetch_size_exceeds_max_rows__);

    maxRows_ = maxRows;
  }

  /**
   * If escape scanning is on (the default), the driver will do
   * escape substitution before sending the SQL to the database.
   *
   * Note: Since prepared statements have usually been parsed prior
   * to making this call, disabling escape processing for prepared
   * statements will likely have no affect.
   *
   * @param enable true to enable; false to disable
   * @throws java.sql.SQLException if a database access error occurs.
   * @author Madhukar Thakur
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public void setEscapeProcessing (boolean enable) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    escapeProcessingEnabled_ = enable;
  }

  /**
   * The queryTimeout limit is the number of seconds the driver will
   * wait for a Statement to execute. If the limit is exceeded, a
   * SQLException is thrown.
   *
   * @return the current query timeout limit in seconds; zero means unlimited
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  public int getQueryTimeout () throws java.sql.SQLException
  {
    return timeout_;
  }

  /**
   * The queryTimeout limit is the number of seconds the driver will
   * wait for a Statement to execute. If the limit is exceeded, a
   * SQLException is thrown.
   *
   * <p><b>InterClient note:</b>
   * Throws DriverNotCapableException.
   * InterBase does not support asynchronous query timeout or cancel.
   *
   * @param seconds the new query timeout limit in seconds; zero means unlimited
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1, not yet supported</font>
   **/
  synchronized public void setQueryTimeout (int seconds) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    throw new DriverNotCapableException (ErrorKey.driverNotCapable__query_timeout__);
  }

  /**
   * Cancel can be used by one thread to cancel a statement that
   * is being executed by another thread.
   *
   * <p><b>InterClient note:</b>
   * Throws DriverNotCapableException.
   * InterBase does not support asynchronous query timeout or cancel.
   *
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1, not yet supported</font>
   **/
  synchronized public void cancel () throws java.sql.SQLException
  {
    checkForClosedStatement ();

    throw new DriverNotCapableException (ErrorKey.driverNotCapable__asynchronous_cancel__);
  }

  /**
   * The first warning reported by calls on this Statement is
   * returned.  A Statment's execute methods clear its SQLWarning
   * chain. Subsequent Statement warnings will be chained to this
   * SQLWarning.
   *
   * <p>The warning chain is automatically cleared each time
   * a statement is (re)executed.
   *
   * <P><B>Note:</B> If you are processing a ResultSet then any
   * warnings associated with ResultSet reads will be chained on the
   * ResultSet object.
   *
   * @return the first SQLWarning or null
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  public java.sql.SQLWarning getWarnings () throws java.sql.SQLException
  {
    return sqlWarnings_;
  }

  /**
   * After this call, getWarnings returns null until a new warning is
   * reported for this Statement.
   *
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public void clearWarnings () throws java.sql.SQLException
  {
    sqlWarnings_ = null;
  }

  // seen by subclasses only
  void setWarning (java.sql.SQLWarning e)
  {
    if (sqlWarnings_ != null)
      sqlWarnings_.setNextException (e);
    else
      sqlWarnings_ = e;
  }

  // !!! comment this?
  /**
   * setCursorname defines the SQL cursor name that will be used by
   * subsequent Statement execute methods. This name can then be
   * used in SQL positioned update/delete statements to identify the
   * current row in the ResultSet generated by this statement.  If
   * the database doesn't support positioned update/delete, this
   * method is a noop.  To insure that a cursor has the proper isolation
   * level to support update, the cursor's select statement should be
   * of the form 'select for update ...'. If the 'for update' clause is
   * omitted the positioned updates may fail.
   *
   * <P><B>Note:</B> By definition, positioned update/delete
   * execution must be done by a different Statement than the one
   * which generated the ResultSet being used for positioning. Also,
   * cursor names must be unique within a Connection.
   *
   * @param name the new cursor name.
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public void setCursorName (String cursorName) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    if (cursorName == null)
      cursorName_ = "";
    else
      cursorName_ = cursorName;
  }

  //----------------------- Multiple Results --------------------------

  /**
   * Execute a SQL statement that may return multiple results.
   * Under some (uncommon) situations a single SQL statement may return
   * multiple result sets and/or update counts.  Normally you can ignore
   * this, unless you're executing a stored procedure that you know may
   * return multiple results, or unless you're dynamically executing an
   * unknown SQL string.  The "execute", "getMoreResults", "getResultSet"
   * and "getUpdateCount" methods let you navigate through multiple results.
   *
   * The "execute" method executes a SQL statement and indicates the
   * form of the first result.  You can then use getResultSet or
   * getUpdateCount to retrieve the result, and getMoreResults to
   * move to any subsequent result(s).
   *
   * @param sql any SQL statement
   * @return true if the next result is a ResultSet; false if it is
   *    an update count or there are no more results
   * @throws java.sql.SQLException if a database access error occurs.
   * @see #getResultSet
   * @see #getUpdateCount
   * @see #getMoreResults
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public boolean execute (String sql) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    clearWarnings ();

    if (resultSet_ != null) {
      resultSet_.local_Close ();
      resultSet_ = null;
    }

    updateCountStack_ = null;

    checkForEmptySQL (sql);

    if (escapeProcessingEnabled_) {
      EscapeProcessor escapeProcessor = new EscapeProcessor();
      sql = escapeProcessor.doEscapeProcessing(sql);
    }

    connection_.transactionStartedOnClient_ = true;

    boolean isQuery = remote_EXECUTE_STATEMENT (sql);

    connection_.transactionStartedOnServer_ = true;
    openOnServer_ = true;
    resultSetStack_ = resultSet_;

    return isQuery;
  }

  private boolean remote_EXECUTE_STATEMENT (String sql) throws java.sql.SQLException
  {
    MessageBufferOutputStream sendMsg = jdbcNet_.createMessage ();

    sendMsg.writeByte (MessageCodes.EXECUTE_STATEMENT__);
    send_StatementExecuteData (sendMsg, sql);

    RecvMessage recvMsg = null;
    try {
      recvMsg = jdbcNet_.sendAndReceiveMessage (sendMsg);

      if (!recvMsg.get_SUCCESS ()) {
	throw recvMsg.get_EXCEPTIONS ();
      }

      // statementRef_ must be set before calling prefetch!
      statementRef_ = recvMsg.readInt ();

      if (!recvMsg.getHeaderEndOfStream ())
	remote_sendPrefetch ();

      int count = recvMsg.readUnsignedByte ();

      if (count > 1) { // for future use, this is the number of result sets
	throw new RemoteProtocolException (ErrorKey.remoteProtocol__unexpected_token_from_server_0__,
					   104);
      }

      if (count == 1) { // query statement
	int resultCols = recvMsg.readUnsignedShort ();

	resultSet_ = new ResultSet (this, jdbcNet_, recvMsg, resultCols, true);
        resultSet_.cursorName_ = cursorName_;
	resultSet_.recv_ResultMetaData (recvMsg);
        resultSet_.numRows_ = recvMsg.readInt ();
        resultSet_.setNumDataPositions (resultCols);
        resultSet_.saveRowPosition ();
	// recvMsg remains associated with resultSet_
      }

      if (count == 0) { // update statement
	updateCountStack_ = new Integer (recvMsg.readInt ());
	setWarning (recvMsg.get_WARNINGS ());
	jdbcNet_.destroyRecvMessage (recvMsg);
      }

      return (resultSet_ != null);
    }
    catch (java.sql.SQLException e) {
      if (resultSet_ != null)
        resultSet_.local_Close ();
      resultSet_ = null;
      updateCountStack_ = null;
      jdbcNet_.destroyRecvMessage (recvMsg);
      throw e;
    }
  }

  /**
   * getResultSet returns the current result as a ResultSet.  It
   * should only be called once per result.
   *
   * @return the current result as a ResultSet; null if the result
   *    is an update count or there are no more results
   * @throws java.sql.SQLException if a database access error occurs.
   * @see #execute
   * @since <font color=red>JDBC 1</font>
   **/
  public java.sql.ResultSet getResultSet () throws java.sql.SQLException
  {
    checkForClosedStatement ();

    return resultSetStack_;
  }

  /**
   *  getUpdateCount returns the current result as an update count;
   *  if the result is a ResultSet or there are no more results, -1
   *  is returned.  It should only be called once per result.
   *
   * @return the current result as an update count; -1 if it is a
   *    ResultSet or there are no more results
   * @throws java.sql.SQLException if a database access error occurs.
   * @see #execute
   * @since <font color=red>JDBC 1</font>
   **/
  public int getUpdateCount () throws java.sql.SQLException
  {
    checkForClosedStatement ();

    if (updateCountStack_ == null)
      return -1;

    return updateCountStack_.intValue ();
  }

  /**
   * getMoreResults moves to a Statement's next result.  It returns true if
   * this result is a ResultSet.  getMoreResults also implicitly
   * closes any current ResultSet obtained with getResultSet.
   *
   * There are no more results when (!getMoreResults() &&
   * (getUpdateCount() == -1)
   *
   * @return true if the next result is a ResultSet; false if it is
   *   an update count or there are no more results
   * @throws java.sql.SQLException if a database access error occurs.
   * @see #execute
   * @since <font color=red>JDBC 1</font>
   **/
  synchronized public boolean getMoreResults () throws java.sql.SQLException
  {
    checkForClosedStatement ();

    if (resultSet_ != null)
      resultSet_.close ();

    // Pop the stack
    resultSetStack_ = null;

    // We don't support multiple result sets
    return false;
  }

  // seen only by subclasses
  int estimateSendBufferSize (int sqlLength)
  {
    // 50 accounts for all StatementExecuteData
    return 50 + cursorName_.length() + sqlLength;
  }

  void send_StatementExecuteData (MessageBufferOutputStream sendMsg,
                                  String sql) throws java.sql.SQLException
  {
    sendMsg.writeInt (statementRef_);
    sendMsg.writeLDSQLText (cursorName_);
    connection_.send_TransactionConfigData (sendMsg);
    sendMsg.writeLDChars (sql);
    sendMsg.writeShort (timeout_);
    sendMsg.writeShort (maxFieldSize_);
    sendMsg.writeInt (fetchSize_);
  }

  //--------------------------JDBC 2.0-----------------------------


  /**
   * Give a hint as to the direction in which the rows in a result set
   * will be processed. The hint applies only to result sets created
   * using this Statement object.  The default value is
   * ResultSet.FETCH_FORWARD.
   *
   * @param direction the initial direction for processing rows
   * @throws java.sql.SQLException if a database access error occurs or direction
   *   is not one of ResultSet.FETCH_FORWARD, ResultSet.FETCH_REVERSE, or
   *   ResultSet.FETCH_UNKNOWN
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public void setFetchDirection (int direction) throws java.sql.SQLException
  {
    throw new DriverNotCapableException (ErrorKey.driverNotCapable__jdbc2_not_yet_supported__);
  }
  //*end jre12*

  /**
   * Determine the fetch direction.
   *
   * @return the default fetch direction
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public int getFetchDirection () throws java.sql.SQLException
  {
    throw new DriverNotCapableException (ErrorKey.driverNotCapable__jdbc2_not_yet_supported__);
  }
  //*end jre12*

  /**
   * Give the JDBC driver a hint as to the number of rows that should
   * be fetched from the database when more rows are needed.  The number
   * of rows specified only affects result sets created using this
   * statement. If the value specified is zero, then the hint is ignored.
   * The default value is zero.
   *
   * <p><b>InterClient note:</b>
   * The default of zero lets the driver decide.
   * By default, InterClient prefetches as many rows as can fill
   * up a 128K buffer.
   *
   * @param rows the number of rows to fetch
   * @throws java.sql.SQLException if a database access error occurs, or the
   *    condition 0 <= rows <= this.getMaxRows() is not satisfied.
   * @since <font color=red>JDBC 2, since InterClient 1.50</font>
   **/
  synchronized public void setFetchSize (int rows) throws java.sql.SQLException
  {
    checkForClosedStatement ();

    if (rows < 0)
      throw new InvalidArgumentException (ErrorKey.invalidArgument__negative_row_fetch_size__);

    if ((maxRows_ > 0) && (rows > maxRows_))
      throw new InvalidArgumentException (ErrorKey.invalidArgument__fetch_size_exceeds_max_rows__);

    fetchSize_ = rows;
  }

  /**
   * Determine the default fetch size.
   *
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public int getFetchSize() throws java.sql.SQLException
  {
      return fetchSize_;
  }
  //*end jre12*

  /**
   * Determine the result set concurrency.
   *
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public int getResultSetConcurrency() throws java.sql.SQLException
  {
    throw new DriverNotCapableException (ErrorKey.driverNotCapable__jdbc2_not_yet_supported__);
  }
  //*end jre12*

  /**
   * Determine the result set type.
   *
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public int getResultSetType() throws java.sql.SQLException
  {
    throw new DriverNotCapableException (ErrorKey.driverNotCapable__jdbc2_not_yet_supported__);
  }
  //*end jre12*

  /**
   * Adds a SQL command to the current batch of commmands for the statement.
   * This method is optional.
   *
   * @param sql typically this is a static SQL INSERT or UPDATE statement
   * @throws java.sql.SQLException if a database access error occurs, or the
   *    driver does not support batch statements
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public void addBatch( String sql ) throws java.sql.SQLException
  {
    throw new DriverNotCapableException (ErrorKey.driverNotCapable__jdbc2_not_yet_supported__);
  }
  //*end jre12*

  /**
   * Make the set of commands in the current batch empty.
   * This method is optional.
   *
   * @throws java.sql.SQLException if a database access error occurs, or the
   *   driver does not support batch statements
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public void clearBatch() throws java.sql.SQLException
  {
    throw new DriverNotCapableException (ErrorKey.driverNotCapable__jdbc2_not_yet_supported__);
  }
  //*end jre12*

  /**
   * Submit a batch of commands to the database for execution.
   * This method is optional.
   *
   * @return an array of update counts containing one element for each
   *   command in the batch.  The array is ordered according
   *   to the order in which commands were inserted into the batch
   * @throws java.sql.SQLException if a database access error occurs, or the
   *   driver does not support batch statements
   * @since <font color=red>JDBC 2, not yet supported</font>
   **/ //*start jre12*
  synchronized public int[] executeBatch() throws java.sql.SQLException
  {
    throw new DriverNotCapableException (ErrorKey.driverNotCapable__jdbc2_not_yet_supported__);
  }
  //*end jre12*

  /**
   * Returns the Connection that produced this Statement.
   *
   * @throws java.sql.SQLException if a database access error occurs.
   * @since <font color=red>JDBC 2, since InterClient 1.50</font>
   **/
  synchronized public java.sql.Connection getConnection() throws java.sql.SQLException
  {
    return connection_;
  }
}

