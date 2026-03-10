package com.github.animetrity.onlinepbx.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PbxCallHistoryResponse {
    private String status;
    private List<Call> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Call {
        private String uuid;
        @JsonProperty("caller_id_name") private String callerIdName;
        @JsonProperty("caller_id_number") private String callerIdNumber;
        @JsonProperty("destination_number") private String destinationNumber;
        @JsonProperty("from_host") private String fromHost;
        @JsonProperty("to_host") private String toHost;
        @JsonProperty("start_stamp") private long startStamp;
        @JsonProperty("end_stamp") private long endStamp;
        private int duration;
        @JsonProperty("user_talk_time") private int userTalkTime;
        @JsonProperty("hangup_cause") private String hangupCause;
        private String accountcode;
        private String gateway;
        @JsonProperty("quality_score") private int qualityScore;
        private boolean contacted;
        private List<Event> events;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String type;
        private long timestamp;
        private String digit;
        private String number;
        @JsonProperty("answered_stamp") private long answeredStamp;
        @JsonProperty("end_stamp") private long endStamp;
        private String uuid;
        @JsonProperty("to_uuid") private String toUuid;
    }
}