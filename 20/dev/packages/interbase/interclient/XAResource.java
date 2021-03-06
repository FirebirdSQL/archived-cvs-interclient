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
 *
 * $Id$
 */

package interbase.interclient;

/**
 * The XAResource interface is a Java mapping of the industry standard
 * XA interface based on the X/Open CAE Specification (Distributed
 * Transaction Processing: The XA Specification).
 *
 * <p>The XA interface defines the contract between a Resource Manager
 * and a Transaction Manager in a distributed transaction processing
 * (DTP) environment. A JDBC driver or a JMS provider implements the
 * XA interface to support association between a global transaction
 * and a database or message service connection.
 *
 * <p>The XAResource interface can be supported by any transactional
 * resource that is intended to be used by application programs in an
 * environment where transactions are controlled by an external
 * transaction manager. An example of such a resource is a database
 * management system. An application may access data through multiple
 * database connections. Each database connection has a resource manager
 * that is registered with the transaction manager. The transaction
 * manager obtains an XAResource for each resource manager participating
 * in a global transaction. It uses the start method to associate the
 * global transaction with the XA resource, and it uses the end method to
 * dissociate the transaction from the resource. The resource manager is
 * responsible for associating the global transaction to all work performed
 * on its data between the start and end method invocation.
 *
 * <p>At transaction commit time, these XA resource managers are informed by
 * the transaction manager to prepare, commit, or rollback a transaction
 * according to the two phase commit protocol.
 *
 * @see javax.sql.XAConnection
 * @see javax.transaction.xa.Xid
 * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
 * @version Std Ext 0.7, JTA 0.9
 **/
public class XAResource implements javax.transaction.xa.XAResource
{
  /**
   * End a recovery scan.
   **/
  public final static int TMENDRSCAN =   0x00800000;

  /**
   * Dissociates the caller and mark the transaction branch
   * rollback-only.
   **/
  public final static int TMFAIL =       0x20000000;

  /**
   * Caller is joining existing transaction branch.
   **/
  public final static int TMJOIN =       0x00200000;

  /**
   * Use TMNOFLAG to indicate no flags value is selected.
   **/
  public final static int TMNOFLAG =     0x00000000;

  /**
   * Caller is using one-phase optimization.
   **/
  public final static int TMONEPHASE =   0x40000000;

  /**
   * Caller is resuming association with with suspended transaction branch.
   **/
  public final static int TMRESUME =     0x08000000;

  /**
   * Start a recovery scan.
   **/
  public final static int TMSTARTRSCAN = 0x01000000;


  /**
   * Dissociate caller from transaction branch.
   **/
  public final static int TMSUCCESS =    0x04000000;


  /**
   * Caller is suspending (not ending) association with transaction branch.
   **/
  public final static int TMSUSPEND =    0x02000000;

  /**
   * Commit the global transaction specified by xid.
   *
   * @param xid A global transaction identifier
   *
   * @param onePhase If true, the resource manager should use a one-phase
   * commit protocol to commit the work done on behalf of xid.
   *
   * @throws XAException An error has occurred. Possible XAExceptions
   * are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR,
   * XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
   *
   * <P>If the resource manager did not commit the transaction and the
   *  paramether onePhase is set to true, the resource manager may raise
   *  one of the XA_RB* exceptions. Upon return, the resource manager has
   *  rolled back the branch's work and has released all held resources.
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public void commit (javax.transaction.xa.Xid xid, boolean onePhase) throws javax.transaction.xa.XAException
  {}

  /**
   * Ends the work performed on behalf of a transaction branch.
   * The resource manager dissociates the XA resource from the
   * transaction branch specified and let the transaction be
   * completed.
   *
   * If TMSUSPEND is specified in flags, the transaction branch
   * is temporarily suspended in incomplete state. The transaction
   * context is in suspened state and must be resumed via start
   * with TMRESUME specified.
   *
   * If TMFAIL is specified, the portion of work has failed.
   * The resource manager may mark the transaction as rollback-only
   *
   * If TMSUCCESS is specified, the portion of work has completed
   * successfully.
   *
   * @param xid A global transaction identifier that is the same as
   * what was used previously in the start method.
   *
   * @param flags One of TMSUCCESS, TMFAIL, or TMSUSPEND
   *
   * @throws XAException An error has occurred. Possible XAException
   * values are XAER_RMERR, XAER_RMFAILED, XAER_NOTA, XAER_INVAL,
   * XAER_PROTO, or XA_RB*.
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public void end (javax.transaction.xa.Xid xid, int flags) throws javax.transaction.xa.XAException
  {}

  /**
   * Tell the resource manager to forget about a heuristically
   * completed transaction branch.
   *
   * @param xid A global transaction identifier
   *
   * @throws XAException An error has occurred. Possible exception
   * values are XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or
   * XAER_PROTO.
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public void forget (javax.transaction.xa.Xid xid) throws javax.transaction.xa.XAException
  {}

  /**
   * Obtain the current transaction timeout value set for this
   * XAResource instance. If <CODE>XAResource.setTransactionTimeout</CODE>
   * was not use prior to invoking this method, the return value
   * is the default timeout set for the resource manager; otherwise,
   * the value used in the previous <CODE>setTransactionTimeout</CODE> call is
   * returned.
   *
   * @return the transaction timeout value in seconds.
   *
   * @throws XAException An error has occurred. Possible exception
   * values are XAER_RMERR, XAER_RMFAIL.
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public int getTransactionTimeout() throws javax.transaction.xa.XAException
  {
    return 0; // !!!
  }

  /**
   * Ask the resource manager to prepare for a transaction commit
   * of the transaction specified in xid.
   *
   * @param xid A global transaction identifier
   *
   * @throws XAException An error has occurred. Possible exception
   * values are: XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL,
   * or XAER_PROTO.
   *
   * @return A value indicating the resource manager's vote on the
   * outcome of the transaction. The possible values are: XA_RDONLY
   * or XA_OK. If the resource manager wants to roll back the
   * transaction, it should do so by raising an appropriate XAException
   * in the prepare method.
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public int prepare (javax.transaction.xa.Xid xid) throws javax.transaction.xa.XAException
  { return 0; }

  /**
   * Obtain a list of prepared transaction branches from a resource
   * manager. The transaction manager calls this method during recovery
   * to obtain the list of transaction branches that are currently in
   * prepared or heuristically completed states.
   *
   * @param flag One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS
   * must be used when no other flags are set in flags.
   *
   * @throws XAException An error has occurred. Possible values are
   * XAER_RMERR, XAER_RMFAIL, XAER_INVAL, and XAER_PROTO.
   *
   * @return The resource manager returns zero or more XIDs for the
   * transaction branches that are currently in a prepared or
   * heuristically completed state. If an error occurs during the
   * operation, the resource manager should raise the appropriate
   * XAException.
   *
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public javax.transaction.xa.Xid[] recover (int flag) throws javax.transaction.xa.XAException
  { return null; }

  /**
   * Inform the resource manager to roll back work done on behalf
   * of a transaction branch
   *
   * @param xid A global transaction identifier
   *
   * @throws XAException An error has occurred
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public void rollback (javax.transaction.xa.Xid xid) throws javax.transaction.xa.XAException
  {}

  /**
   * Set the current transaction timeout value for this <CODE>XAResource</CODE>
   * instance. This value overwrites the default transaction timeout
   * value in the resource manager. The newly assigned timeout value
   * is effective for the life of this <CODE>XAResource</CODE> instance unless
   * a new value is set.<P>
   *
   * @param the transaction timeout value in seconds.
   *
   * @return true if transaction timeout value is set successfully; otherwise false.
   *
   * @throws javax.transaction.xa.XAException An error has occurred. Possible exception values
   * are XAER_RMERR, XAER_RMFAIL, or XAER_INVAL.
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/

  //Torsten-start 08-11-2000
  synchronized public void setTransactionTimeout(int seconds) throws javax.transaction.xa.XAException {
//  return false; //not yet supported
  }
  //Torsten-end 08-11-2000

  /**
   * Start work on behalf of a transaction branch specified in xid
   *
   * @param xid A global transaction identifier to be associated
   * with the resource
   *
   * @param flags One of TMNOFLAGS, TMJOIN, or TMRESUME
   *
   * @throws XAException An error has occurred. Possible exceptions
   * are XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_DUPID, XAER_OUTSIDE,
   * XAER_NOTA, XAER_INVAL, or XAER_PROTO.
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   **/
  synchronized public void start (javax.transaction.xa.Xid xid, int flags) throws javax.transaction.xa.XAException
  {}

  //Torsten-start 08-11-2000

  /**
   * This method is called to determine if the resource manager instance
   * represented by the target object is the same as the resouce manager instance
   * represented by the parameter xares.
   *
   * @param xares An XAResource object whose resource manager instance is to be compared
   * with the resource manager instance of the target object.
   *
   * @throws XAException An error has occurred. Possible exceptions
   * are XAER_RMERR, XAER_RMFAIL.
   *
   * @since <font color=red>JDBC 2 Standard Extension, proposed for future release, not yet supported</font>
   * **/

  synchronized public boolean isSameRM(javax.transaction.xa.XAResource xares) throws javax.transaction.xa.XAException {
    return false;
  }

  //Torsten-end 08-11-2000
}
