# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0 (2025-07-12)


### ✨ Features

* Adding gcz group provisioning ([6236e76](https://github.com/danielscholl-osdu/entitlements/commit/6236e76c336580585b8b08444b7ee64b32c505d3))
* Adding gcz group provisioning ([be090bc](https://github.com/danielscholl-osdu/entitlements/commit/be090bca3b86d97d99d24cf0b8cddf519212fd33))


### 🐛 Bug Fixes

* Spring boot netty handler version bump ([a451595](https://github.com/danielscholl-osdu/entitlements/commit/a4515954183568963c8bcbac84824d92cc89a5b9))
* Spring boot netty handler version bump ([6440f11](https://github.com/danielscholl-osdu/entitlements/commit/6440f116da53fa9c835678fdf0db9a0fd8b38e4a))
* Tomcat-core crypto CVE ([adddaab](https://github.com/danielscholl-osdu/entitlements/commit/adddaab389cd078cd634cf45fea6ce381140480b))
* Tomcat-core crypto CVE ([62eb243](https://github.com/danielscholl-osdu/entitlements/commit/62eb243d33afe1634284796719bf8179c3fa83d3))


### 🔧 Miscellaneous

* Complete repository initialization ([842d56a](https://github.com/danielscholl-osdu/entitlements/commit/842d56ae4c94fa230f3c459644a78786545415f7))
* Copy configuration and workflows from main branch ([2765937](https://github.com/danielscholl-osdu/entitlements/commit/2765937411868f98c5a8ca19efb80fc370a1a652))
* Deleting aws helm chart ([0ff77e4](https://github.com/danielscholl-osdu/entitlements/commit/0ff77e429d5acc3950f0244d294aec11b903897f))
* Deleting aws helm chart ([4bd7ce5](https://github.com/danielscholl-osdu/entitlements/commit/4bd7ce5e61a5bbce858560c5f60e61952ed3b208))
* Enabling impersonation tests on AWS ([16e4193](https://github.com/danielscholl-osdu/entitlements/commit/16e4193c3726936aa7c653109e31414abcf190a8))
* Enabling impersonation tests on AWS ([ca4669f](https://github.com/danielscholl-osdu/entitlements/commit/ca4669f71d817da6c635c604f904e1954fedfe64))
* Removing chart copy from prepare-dist.sh ([7b58645](https://github.com/danielscholl-osdu/entitlements/commit/7b586459b9dc8d621936b29cf0b6182d3442cdb2))
* Removing helm copy from aws buildspec ([19b5484](https://github.com/danielscholl-osdu/entitlements/commit/19b54842c441037a2d153c090f1792d9c0773941))

## [2.0.0] - Major Workflow Enhancement & Documentation Release

### ✨ Features
- **Comprehensive MkDocs Documentation Site**: Complete documentation overhaul with GitHub Pages deployment
- **Automated Cascade Failure Recovery**: System automatically recovers from cascade workflow failures
- **Human-Centric Cascade Pattern**: Issue lifecycle tracking with human notifications for critical decisions
- **Integration Validation**: Comprehensive validation system for cascade workflows
- **Claude Workflow Integration**: Full Claude Code CLI support with Maven MCP server integration
- **GitHub Copilot Enhancement**: Java development environment setup and firewall configuration
- **Fork Resources Staging Pattern**: Template-based staging for fork-specific configurations
- **Conventional Commits Validation**: Complete validation system with all supported commit types
- **Enhanced PR Label Management**: Simplified production PR labels with automated issue closure
- **Meta Commit Strategy**: Advanced release-please integration for better version management
- **Push Protection Handling**: Sophisticated upstream secrets detection and resolution workflows

### 🔨 Build System
- **Workflow Separation Pattern**: Template development vs. fork instance workflow isolation
- **Template Workflow Management**: 9 comprehensive template workflows for fork management
- **Enhanced Action Reliability**: Improved cascade workflow trigger reliability with PR event filtering
- **Base64 Support**: Enhanced create-enhanced-pr action with encoding capabilities

### 📚 Documentation
- **Structured MkDocs Site**: Complete documentation architecture with GitHub Pages
- **AI-First Development Docs**: Comprehensive guides for AI-enhanced development
- **ADR Documentation**: 20+ Architectural Decision Records covering all major decisions
- **Workflow Specifications**: Detailed documentation for all 9 template workflows
- **Streamlined README**: Focused quick-start guide directing to comprehensive documentation

### 🛡️ Security & Reliability
- **Advanced Push Protection**: Intelligent handling of upstream repositories with secrets
- **Branch Protection Integration**: Automated branch protection rule management
- **Security Pattern Recognition**: Enhanced security scanning and pattern detection
- **MCP Configuration**: Secure Model Context Protocol integration for AI development

### 🔧 Workflow Enhancements
- **Cascade Monitoring**: Advanced cascade workflow monitoring and SLA management
- **Dependabot Integration**: Enhanced dependabot validation and automation
- **Template Synchronization**: Sophisticated template update propagation system
- **Issue State Tracking**: Advanced issue lifecycle management and tracking
- **GITHUB_TOKEN Standardization**: Improved token handling across all workflows

### ♻️ Code Refactoring
- **Removed AI_EVOLUTION.md**: Migrated to structured ADR approach for better maintainability
- **Simplified README Structure**: Eliminated redundancy between README and documentation site
- **Enhanced Initialization Cleanup**: Improved fork repository cleanup and setup process
- **Standardized Error Handling**: Consistent error handling patterns across all workflows

### 🐛 Bug Fixes
- **YAML Syntax Issues**: Resolved multiline string handling in workflow configurations
- **Release Workflow Compatibility**: Updated to googleapis/release-please-action@v4
- **MCP Server Configuration**: Fixed Maven MCP server connection and configuration issues
- **Cascade Trigger Reliability**: Implemented pull_request_target pattern for better triggering
- **Git Diff Syntax**: Corrected git command syntax in sync-template workflow
- **Label Management**: Standardized label usage across all workflows and templates

## [1.0.0] - Initial Release

### ✨ Features
- Initial release of OSDU Fork Management Template
- Automated fork initialization workflow
- Daily upstream synchronization with AI-enhanced PR descriptions
- Three-branch management strategy (main, fork_upstream, fork_integration)
- Automated conflict detection and resolution guidance
- Semantic versioning and release management
- Template development workflows separation

### 📚 Documentation
- Complete architectural decision records (ADRs)
- Product requirements documentation
- Development and usage guides
- GitHub Actions workflow documentation
