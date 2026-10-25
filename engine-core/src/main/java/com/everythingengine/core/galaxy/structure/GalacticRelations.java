package com.everythingengine.core.galaxy.structure;

import com.everythingengine.core.relation.RelationType;

public final class GalacticRelations {
    public static final RelationType HAS_STRUCTURE = new RelationType("galaxy.has-structure");
    public static final RelationType CONTAINS_SYSTEM = new RelationType("galaxy.contains-system");
    public static final RelationType SYSTEM_CONTAINS_STAR = new RelationType("system.contains-star");
    public static final RelationType SYSTEM_IN_REGION = new RelationType("system.in-region");

    private GalacticRelations() {
    }
}
