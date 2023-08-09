package com.example.servingwebcontent;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // This indicates that any properties not bound in this type should be
                                            // ignored.
public record HackerNewsUsers(
    String about,
    long created,
    String id,
    int karma,
    List submitted
) {
}
