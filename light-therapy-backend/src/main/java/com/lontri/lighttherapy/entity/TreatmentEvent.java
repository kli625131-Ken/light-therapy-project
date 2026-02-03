package com.lontri.lighttherapy.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="treatment_event")
public class TreatmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", nullable=false)
    private Long sessionId;

    @Column(name="event_time", nullable=false)
    private LocalDateTime eventTime;

    @Column(name="event_type", nullable=false, length=32)
    private String eventType;

    @Column(length=1024)
    private String message;

    @Column(name="detail_json", columnDefinition="json")
    private String detailJson;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	public LocalDateTime getEventTime() {
		return eventTime;
	}

	public void setEventTime(LocalDateTime eventTime) {
		this.eventTime = eventTime;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDetailJson() {
		return detailJson;
	}

	public void setDetailJson(String detailJson) {
		this.detailJson = detailJson;
	}

    
}
