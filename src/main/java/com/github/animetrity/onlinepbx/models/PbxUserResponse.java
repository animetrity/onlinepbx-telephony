package com.github.animetrity.onlinepbx.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PbxUserResponse {
    private String status;

    // ACCEPT_SINGLE_VALUE_AS_ARRAY гарантує, що ми завжди отримаємо List,
    // навіть якщо API поверне лише одного користувача без квадратних дужок [].
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<User> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private int num;
        private String name;
        private String tr1;
        private String tr2;
        private String tr3;
        private int delay1;
        private int delay2;
        private int delay3;
        private boolean enabled;
    }
}