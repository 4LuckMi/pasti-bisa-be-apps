package com.pasti_bisa.backend.shared.web;

import java.util.List;

/** Bounded collection contract shared by internal screens. */
public record PageResponse<T>(List<T> items,long total,int offset,int limit) {}
