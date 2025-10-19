#!/bin/bash
set -e

# Color codes for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Screenshot definitions
declare -a SCREENSHOTS=(
    "main_screen.png:Main screen - RecordShot tab"
    "main_screen_2.png:Main screen - alternative view or state"
    "bean_management.png:Bean Management screen"
    "bean_photo.png:Bean with photo detail"
    "grinder_settings.png:Grinder settings/configuration"
    "shot_history.png:Shot History tab"
    "shot_details.png:Shot details view"
    "shot_analysis.png:Shot analysis screen"
    "filter_shots.png:Shot history with filters applied"
    "light_mode.png:App in light mode (toggle in settings)"
)

# Track captured and skipped screenshots
declare -a CAPTURED=()
declare -a SKIPPED=()

# Flag to track if interrupted
INTERRUPTED=0

# Cleanup function to kill feh processes
cleanup() {
    pkill -f "feh.*docs/" 2>/dev/null || true
}

# Interrupt handler
handle_interrupt() {
    INTERRUPTED=1
    cleanup
    echo -e "\n${RED}Interrupted${NC}"
    exit 130
}

trap cleanup EXIT
trap handle_interrupt INT TERM

# Function to read a single keypress
read_key() {
    local key
    # Check if interrupted before reading
    if [[ $INTERRUPTED -eq 1 ]]; then
        return 1
    fi
    
    IFS= read -rsn1 key
    local read_status=$?
    
    # Check if interrupted after reading
    if [[ $INTERRUPTED -eq 1 ]] || [[ $read_status -ne 0 ]]; then
        return 1
    fi
    
    # Handle escape sequences (ESC key)
    if [[ $key == $'\x1b' ]]; then
        # Read the rest of the escape sequence if any
        read -rsn2 -t 0.01 key
        echo "esc"
    elif [[ $key == "" ]]; then
        # Enter key
        echo "enter"
    else
        # Any other key
        echo "$key"
    fi
}

# Function to wait for user confirmation to continue or skip
wait_for_language_confirmation() {
    local lang_name=$1
    
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}Change device language to: ${lang_name}${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "Press ${GREEN}Enter${NC} to continue with ${lang_name} screenshots"
    echo -e "Press ${RED}Esc${NC} to skip all ${lang_name} screenshots"
    echo ""
    
    local key=$(read_key)
    if [[ $? -ne 0 ]] || [[ $INTERRUPTED -eq 1 ]]; then
        exit 130
    fi
    if [[ $key == "esc" ]]; then
        echo -e "${YELLOW}Skipping all ${lang_name} screenshots${NC}"
        return 1
    fi
    return 0
}

# Function to capture a single screenshot
capture_screenshot() {
    local filename=$1
    local description=$2
    local lang_dir=$3
    local index=$4
    local total=$5
    local lang_label=$6
    
    local filepath="${lang_dir}/${filename}"
    
    # Display existing screenshot with feh in background
    if [[ -f "$filepath" ]]; then
        feh --scale-down --auto-zoom --title "Current: $filename" "$filepath" &
        local feh_pid=$!
    fi
    
    while true; do
        echo ""
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${BLUE}Screenshot ${index}/${total} - ${lang_label}${NC}"
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "File: ${GREEN}${filename}${NC}"
        echo -e "Navigate to: ${YELLOW}${description}${NC}"
        echo ""
        echo -e "Press ${GREEN}Enter${NC} to capture screenshot"
        echo -e "Press ${YELLOW}r${NC} to retake"
        echo -e "Press ${RED}Esc${NC} to skip"
        echo ""
        
        local key=$(read_key)
        if [[ $? -ne 0 ]] || [[ $INTERRUPTED -eq 1 ]]; then
            if [[ -n ${feh_pid:-} ]]; then
                kill $feh_pid 2>/dev/null || true
            fi
            exit 130
        fi
        
        if [[ $key == "enter" ]]; then
            # Capture screenshot
            echo -e "${BLUE}Capturing screenshot...${NC}"
            if adb exec-out screencap -p > "$filepath"; then
                echo -e "${GREEN}✓ Screenshot captured: ${filepath}${NC}"
                CAPTURED+=("$lang_label: $filename")
            else
                echo -e "${RED}✗ Failed to capture screenshot${NC}"
                SKIPPED+=("$lang_label: $filename (error)")
            fi
            
            # Kill feh
            if [[ -n ${feh_pid:-} ]]; then
                kill $feh_pid 2>/dev/null || true
            fi
            break
            
        elif [[ $key == "r" ]]; then
            # Retake - just loop again
            echo -e "${YELLOW}Retaking screenshot...${NC}"
            continue
            
        elif [[ $key == "esc" ]]; then
            echo -e "${YELLOW}Skipped: ${filename}${NC}"
            SKIPPED+=("$lang_label: $filename")
            
            # Kill feh
            if [[ -n ${feh_pid:-} ]]; then
                kill $feh_pid 2>/dev/null || true
            fi
            break
        fi
    done
}

# Function to process all screenshots for a language
process_language() {
    local lang_dir=$1
    local lang_label=$2
    local lang_name=$3
    
    # Wait for language confirmation
    if ! wait_for_language_confirmation "$lang_name"; then
        # User pressed Esc, skip this language
        for screenshot_entry in "${SCREENSHOTS[@]}"; do
            local filename="${screenshot_entry%%:*}"
            SKIPPED+=("$lang_label: $filename (language skipped)")
        done
        return
    fi
    
    local total=${#SCREENSHOTS[@]}
    local index=1
    
    for screenshot_entry in "${SCREENSHOTS[@]}"; do
        local filename="${screenshot_entry%%:*}"
        local description="${screenshot_entry##*:}"
        
        capture_screenshot "$filename" "$description" "$lang_dir" "$index" "$total" "$lang_label"
        ((index++))
    done
    
    echo -e "${GREEN}✓ Completed ${lang_label} screenshots${NC}"
}

# Print summary
print_summary() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Summary${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    echo -e "${GREEN}Captured (${#CAPTURED[@]}):${NC}"
    if [[ ${#CAPTURED[@]} -eq 0 ]]; then
        echo "  None"
    else
        for item in "${CAPTURED[@]}"; do
            echo "  ✓ $item"
        done
    fi
    
    echo ""
    echo -e "${YELLOW}Skipped (${#SKIPPED[@]}):${NC}"
    if [[ ${#SKIPPED[@]} -eq 0 ]]; then
        echo "  None"
    else
        for item in "${SKIPPED[@]}"; do
            echo "  - $item"
        done
    fi
    
    echo ""
    echo -e "${GREEN}Done!${NC}"
}

# Main script
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}CoffeeShotTimer Screenshot Update Script${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

if ! command -v adb &> /dev/null; then
    echo -e "${RED}✗ adb not found. Please install Android SDK platform tools.${NC}"
    exit 1
fi

if ! command -v feh &> /dev/null; then
    echo -e "${RED}✗ feh not found. Please install it: sudo pacman -S feh${NC}"
    exit 1
fi

if ! adb devices | grep -q "device$"; then
    echo -e "${RED}✗ No Android device connected. Please connect a device and enable USB debugging.${NC}"
    exit 1
fi

if [[ ! -d "docs/en-US" ]] || [[ ! -d "docs/de-DE" ]]; then
    echo -e "${RED}✗ Screenshot directories not found (docs/en-US or docs/de-DE)${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites met${NC}"
echo ""

# Process English screenshots
process_language "docs/en-US" "en-US" "English"

echo ""

# Process German screenshots
process_language "docs/de-DE" "de-DE" "German (Deutsch)"

# Print summary
print_summary
