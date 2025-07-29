package com.mailscheduler.domain.model.email;

/**
 * Classifies emails by their purpose and origin in the mailing system.
 * <p>
 *     This enum distinguishes between first-contact emails and follow-ups, as well as between emails generated from
 *     within the system and those imported from external sources.
 * </p>
 */
public enum EmailType {
    /**
     * First email in a sequence initiated from within the system.
     */
    INITIAL,

    /**
     * Follow-up email in a sequence initiated from within the system.
     */
    FOLLOW_UP,

    /**
     * First email in a sequence imported from external sources.
     */
    EXTERNALLY_INITIAL,

    /**
     * Follow-up email in a sequence imported from external sources.
     */
    EXTERNALLY_FOLLOW_UP;


    /**
     * Determines if this email type is part of a follow-up sequence.
     *
     * @return true if this is a follow-up email type
     */
    public boolean isFollowUp() {
        return this == FOLLOW_UP || this == EXTERNALLY_FOLLOW_UP;
    }

    /**
     * Determines if this email type represents an initial contact.
     *
     * @return true if this is an initial email type
     */
    public boolean isInitial() {
        return this == INITIAL || this == EXTERNALLY_INITIAL;
    }

    /**
     * Determines if this email type was created externally.
     *
     * @return true if this is an externally created email type
     */
    public boolean isExternal() {
        return this == EXTERNALLY_INITIAL || this == EXTERNALLY_FOLLOW_UP;
    }

    @Override
    public String toString() {
        return name();
    }
}
