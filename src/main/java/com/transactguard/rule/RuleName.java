package com.transactguard.rule;

/**
 * Closed set of AML detection rules.
 *
 * <p>
 * Every rule in the system must be registered here. This is intentional —
 * AML rules must be auditable, version-controlled, and approved by
 * compliance before activation. No ad-hoc rule injection is permitted.
 * </p>
 */
public enum RuleName {
    HIGH_VALUE,
    STRUCTURING,
    VELOCITY,
    ROUND_AMOUNT
}
