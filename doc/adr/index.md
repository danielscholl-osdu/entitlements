# ADR Catalog

Architecture Decision Records for Fork Management Template

## Index

| ID  | Title                                      | Status   | Date       | Details |
| --- | ------------------------------------------ | -------- | ---------- | ------- |
| 001 | Three-Branch Fork Management Strategy      | Accepted | 2025-05-28 | [ADR-001](001-three-branch-strategy.md) |
| 002 | GitHub Actions-Based Automation            | Accepted | 2025-05-28 | [ADR-002](002-github-actions-automation.md) |
| 003 | Template Repository Pattern                | Accepted | 2025-05-28 | [ADR-003](003-template-repository-pattern.md) |
| 004 | Release Please for Version Management      | Accepted | 2025-05-28 | [ADR-004](004-release-please-versioning.md) |
| 005 | Automated Conflict Management Strategy     | Accepted | 2025-05-28 | [ADR-005](005-conflict-management.md) |
| 006 | Two-Workflow Initialization Pattern        | Accepted | 2025-05-28 | [ADR-006](006-two-workflow-initialization.md) |
| 007 | Initialization Workflow Bootstrap Pattern  | Proposed | 2025-05-29 | [ADR-007](007-initialization-workflow-bootstrap.md) |

## Overview

These Architecture Decision Records document the key design choices made in the Fork Management Template project. Each ADR explains the context, decision, rationale, and consequences of significant architectural choices that enable automated management of long-lived forks of upstream repositories.

## Quick Reference

### Core Architecture Decisions

**Three-Branch Strategy (ADR-001)**
- `main`: Stable production branch
- `fork_upstream`: Tracks upstream changes
- `fork_integration`: Conflict resolution workspace

**Automation Framework (ADR-002)**
- GitHub Actions for all workflow automation
- Self-configuring template repository pattern
- Scheduled and event-driven synchronization

**Two-Workflow Initialization (ADR-006)**
- Separated user interaction from repository setup
- Issue-driven configuration with progress updates
- Simplified state management and error handling

**Workflow Bootstrap Pattern (ADR-007)**
- Self-updating initialization workflows
- Ensures latest fixes are always available
- Solves the template version bootstrap problem

**Version Management (ADR-004)**
- Release Please with Conventional Commits
- Automated semantic versioning
- Upstream version reference tracking