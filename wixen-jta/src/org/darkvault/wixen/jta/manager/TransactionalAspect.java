package org.darkvault.wixen.jta.manager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRequiredException;

import org.darkvault.wixen.jta.Transactional.TxType;
import org.osgi.service.log.LogService;

public class TransactionalAspect implements InvocationHandler {

	public TransactionalAspect(Object service) {
		_service = service;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (Object.class == method.getDeclaringClass()) {
			String name = method.getName();

			if ("equals".equals(name)) {
				return proxy == args[0];
			}
			else if ("hashCode".equals(name)) {
				return System.identityHashCode(proxy);
			}
			else if ("toString".equals(name)) {
				return
					proxy.getClass().getName() + "@" +
						Integer.toHexString(System.identityHashCode(proxy)) +
							", with InvocationHandler " + this;
			}
			else {
				throw new IllegalStateException(String.valueOf(method));
			}
		}

		TxType txType = StrategyHelper.determineStrategy(method, _service.getClass());

		if (txType == null) {
			try {
				return method.invoke(_service, args);
			}
			catch (InvocationTargetException e){
				throw e.getTargetException();
			}
		}

		Transaction transaction = null;

		switch (txType) {
		case SUPPORTS:
			try {
				return method.invoke(_service, args);
			}
			catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof RuntimeException) {
					if (Status.STATUS_ACTIVE == _transactionManager.getStatus()) {
						_transactionManager.setRollbackOnly();
					}
				}

				throw e.getTargetException();
			}
		case MANDATORY:
			if (Status.STATUS_ACTIVE != _transactionManager.getStatus()) {
				throw new TransactionRequiredException();
			}

			try {
				return method.invoke(_service, args);
			}
			catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof RuntimeException) {
					_transactionManager.setRollbackOnly();
				}

				throw e.getTargetException();
			}
		case NEVER:
			if (Status.STATUS_ACTIVE == _transactionManager.getStatus()) {
				throw new InvalidTransactionException();
			}

			return method.invoke(_service, args);
		case NOT_SUPPORTED:
			if (Status.STATUS_ACTIVE == _transactionManager.getStatus()) {
				transaction = _transactionManager.suspend();
			}

			try {
				return method.invoke(_service, args);
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
			finally {
				if (transaction != null) {
					_transactionManager.resume(transaction);
				}
			}
		case REQUIRED:
			boolean transactionCreated = false;

			if (Status.STATUS_ACTIVE != _transactionManager.getStatus()) {
				_transactionManager.begin();
				transactionCreated = true;
			}

			try {
				return method.invoke(_service, args);
			}
			catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof RuntimeException) {
					_transactionManager.setRollbackOnly();
				}

				throw e.getTargetException();
			}
			finally {
				if (transactionCreated) {
					try {
						if (_transactionManager.getStatus() == Status.STATUS_ACTIVE) {
							_transactionManager.commit();
						}
						else {
							_transactionManager.rollback();
						}
					}
					catch (Throwable t) {
						_logService.log(LogService.LOG_ERROR, "Failed to complete transaction ", t);

						throw new RuntimeException(t);
					}
				}
			}
		case REQUIRES_NEW:
			if (Status.STATUS_ACTIVE == _transactionManager.getStatus()) {
				transaction = _transactionManager.suspend();
			}

			_transactionManager.begin();

			try {
				return method.invoke(_service, args);
			}
			catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof RuntimeException) {
					_transactionManager.setRollbackOnly();
				}

				throw e.getTargetException();
			}
			finally {
				try {
					if (_transactionManager.getStatus() == Status.STATUS_ACTIVE) {
						_transactionManager.commit();
					}
					else {
						_transactionManager.rollback();
					}
				}
				catch (Throwable t) {
					_logService.log(LogService.LOG_ERROR, "Failed to complete transaction ", t);

					throw new RuntimeException(t);
				}

				if (transaction != null) {
					_transactionManager.resume(transaction);
				}
			}
		default:
			throw new IllegalStateException("Transaction type " + txType + " not supported");
		}
	}

	private volatile LogService _logService;
	private volatile Object _service;
	private volatile TransactionManager _transactionManager;

}