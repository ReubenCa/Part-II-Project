package com.carolang;

import com.carolang.common.ast_nodes.MatchNode;

record MatchStatementBinding(MatchNode node, boolean isHead, int externalPort) {}
