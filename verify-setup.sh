#!/bin/bash

# KMA Library - Phase 0 Verification Script
# This script verifies that all Phase 0 setup is complete and working

echo "🎯 KMA Library - Phase 0 Setup Verification"
echo "=============================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0

# Function to check if command exists
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 is installed"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $1 is not installed"
        ((FAILED++))
        return 1
    fi
}

# Function to check if directory exists
check_directory() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} Directory exists: $1"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} Directory missing: $1"
        ((FAILED++))
        return 1
    fi
}

# Function to check if file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} File exists: $1"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} File missing: $1"
        ((FAILED++))
        return 1
    fi
}

echo "1. Checking Prerequisites"
echo "-------------------------"
check_command "docker"
check_command "docker-compose"
check_command "node"
check_command "npm"
echo ""

echo "2. Checking Project Structure"
echo "-----------------------------"
check_directory "frontend"
check_directory "backend"
check_directory "docs"
check_directory ".github/workflows"
echo ""

echo "3. Checking Frontend Setup"
echo "--------------------------"
check_directory "frontend/src/api"
check_directory "frontend/src/components"
check_directory "frontend/src/hooks"
check_directory "frontend/src/pages"
check_directory "frontend/src/types"
check_directory "frontend/src/utils"
check_file "frontend/package.json"
check_file "frontend/vite.config.ts"
check_file "frontend/tailwind.config.js"
check_file "frontend/Dockerfile"
echo ""

echo "4. Checking Backend Setup"
echo "-------------------------"
check_directory "backend/src/main/kotlin/com/kmalibrary/domain"
check_directory "backend/src/main/kotlin/com/kmalibrary/application"
check_directory "backend/src/main/kotlin/com/kmalibrary/infrastructure"
check_file "backend/build.gradle.kts"
check_file "backend/gradlew"
check_file "backend/src/main/resources/application.yml"
check_file "backend/src/main/resources/db/migration/V1__initial_schema.sql"
check_file "backend/Dockerfile"
echo ""

echo "5. Checking Documentation"
echo "-------------------------"
check_file "README.md"
check_file "docs/IMPLEMENTATION_PLAN.md"
check_file "docs/FUNCTIONALITY_DESIGN.md"
check_file "docs/UI_WIREFRAMES.md"
check_file "docs/PHASE_0_COMPLETION.md"
check_file "docs/adr/001-tech-stack-and-architecture.md"
echo ""

echo "6. Checking Infrastructure"
echo "--------------------------"
check_file "docker-compose.yml"
check_file ".github/workflows/frontend-ci.yml"
check_file ".github/workflows/backend-ci.yml"
echo ""

echo "7. Testing Frontend"
echo "-------------------"
cd frontend 2>/dev/null
if [ -d "node_modules" ]; then
    echo -e "${GREEN}✓${NC} Frontend dependencies installed"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠${NC} Frontend dependencies not installed (run: cd frontend && npm install)"
    ((FAILED++))
fi
cd ..
echo ""

echo "8. Testing Backend"
echo "------------------"
cd backend 2>/dev/null
if [ -f "gradlew" ]; then
    if ./gradlew tasks > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} Gradle wrapper working"
        ((PASSED++))
    else
        echo -e "${YELLOW}⚠${NC} Gradle wrapper needs initialization"
        ((FAILED++))
    fi
else
    echo -e "${RED}✗${NC} Gradle wrapper not found"
    ((FAILED++))
fi
cd ..
echo ""

echo "=============================================="
echo "Verification Complete"
echo "=============================================="
echo -e "Passed: ${GREEN}${PASSED}${NC}"
echo -e "Failed: ${RED}${FAILED}${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Phase 0 setup is complete and verified!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Start infrastructure: docker-compose up -d"
    echo "  2. Start backend: cd backend && ./gradlew bootRun"
    echo "  3. Start frontend: cd frontend && npm run dev"
    echo ""
    echo "  Frontend: http://localhost:5173"
    echo "  Backend:  http://localhost:8080"
    echo "  API Docs: http://localhost:8080/swagger-ui.html"
    exit 0
else
    echo -e "${YELLOW}⚠ Some checks failed. Please review the output above.${NC}"
    exit 1
fi
