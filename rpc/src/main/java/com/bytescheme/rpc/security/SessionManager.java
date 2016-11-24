package com.bytescheme.rpc.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class SessionManager {
	private static Logger LOG = LoggerFactory.getLogger(SessionManager.class);
	private static volatile SessionManager INSTANCE;
	private long sessionLifeTime = 300000L;
	private final ExecutorService executorService;
	private final DelayQueue<Session> sessions = new DelayQueue<>();
	private final Map<String, Session> sessionsMap = new ConcurrentHashMap<>();

	private SessionManager() {
		this.executorService = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			return thread;
		});

		this.executorService.submit(() -> {
			while (true) {
				try {
					process();
				} catch (Exception e) {
					LOG.error("Error occurred in session processing", e);
				}
			}
		});
	}

	public static SessionManager getInstance() {
		if (INSTANCE != null) {
			return INSTANCE;
		}
		synchronized (SessionManager.class) {
			if (INSTANCE == null) {
				INSTANCE = new SessionManager();
			}
		}
		return INSTANCE;
	}

	public long getSessionLifeTime() {
		return sessionLifeTime;
	}

	public void setSessionLifeTime(long sessionLifeTime) {
		this.sessionLifeTime = sessionLifeTime;
	}

	public Session addSessionId(Authentication authentication, String sessionId) {
		Preconditions.checkNotNull(authentication);
		Preconditions.checkArgument(!Strings.isNullOrEmpty(authentication.getUser()));
		Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
		Session session = new Session(authentication, sessionId, System.currentTimeMillis() + sessionLifeTime);
		sessionsMap.put(sessionId, session);
		sessions.add(session);
		return session;
	}

	public Session deleteSession(String sessionId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
		Session session = sessionsMap.get(sessionId);
		if (session == null) {
			return session;
		}
		synchronized (session) {
			sessionsMap.remove(sessionId);
			sessions.remove(sessionId);
		}
		return session;
	}

	public Session getSession(String sessionId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
		return sessionsMap.get(sessionId);
	}

	public void updateSession(String sessionId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
		// Dummy session for key
		Session session = sessionsMap.get(sessionId);
		if (session == null) {
			LOG.warn("Session already expired for ID {}", sessionId);
			return;
		}
		synchronized (session) {
			session = sessionsMap.get(sessionId);
			if (session == null) {
				LOG.warn("Session already expired for ID {}", sessionId);
				return;
			}
			session.setExpiryTime(System.currentTimeMillis() + sessionLifeTime);
		}
	}

	public boolean validateSession(String sessionId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
		return sessionsMap.containsKey(sessionId);
	}

	private void process() throws InterruptedException {
		Session session = sessions.take();
		synchronized (session) {
			if (session.getExpiryTime() > System.currentTimeMillis()) {
				LOG.info("Expiry updated, adding session {} back", session.getId());
				if (sessionsMap.containsKey(session.getId())) {
					sessions.add(session);
				}
				return;
			}
			sessionsMap.remove(session);
			LOG.info("Expired session {}", session.getId());
		}
	}

}
