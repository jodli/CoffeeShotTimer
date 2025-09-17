#!/bin/bash
# Extract priority changes from CHANGELOG.md for Play Store "What's New"
# Usage: ./extract-whatsnew.sh [version|unreleased] [max_chars]
# 
# Priority changes are marked with [*] at the beginning of the bullet point in CHANGELOG.md
# Example: - [*] Major new feature that users will love

set -euo pipefail

VERSION="${1:-unreleased}"
MAX_CHARS="${2:-500}"
CHANGELOG_FILE="${3:-CHANGELOG.md}"

# Function to extract section content
extract_section() {
    local version="$1"
    local file="$2"
    
    if [[ "$version" == "unreleased" ]]; then
        # Extract from [Unreleased] section
        awk '
            BEGIN{p=0}
            /^## \[/{
                if (p==1) exit
                if ($0 ~ /^## \[Unreleased\]/) p=1
            }
            p==1{print}
        ' "$file" | sed '1d' | sed '/^## \[/Q'
    else
        # Extract from specific version section
        awk -v v="$version" '
            BEGIN{p=0}
            /^## \[/{
                if (p==1) exit
                if ($0 ~ "^## \\[(" v ")\\]") p=1
            }
            p==1{print}
        ' "$file" | sed '1d' | sed '/^## \[/Q'
    fi
}

# Function to extract priority items only
extract_priority_items() {
    local content="$1"
    local result=""
    local current_section=""
    
    while IFS= read -r line; do
        # Check if it's a section header (### Added, ### Fixed, etc.)
        if [[ "$line" =~ ^###[[:space:]]+(.*) ]]; then
            current_section="${BASH_REMATCH[1]}"
        # Check if it's a priority item (starts with - [*])
        elif [[ "$line" =~ ^[[:space:]]*-[[:space:]]*\[\*\][[:space:]]*(.*) ]]; then
            # Remove the [*] marker and format as bullet point
            local item="${BASH_REMATCH[1]}"
            if [[ -n "$result" ]]; then
                result="${result}\n"
            fi
            result="${result}• ${item}"
        fi
    done <<< "$content"
    
    echo -e "$result"
}

# Function to extract all items (fallback if no priority items)
extract_all_items() {
    local content="$1"
    local max_chars="$2"
    local result=""
    local char_count=0
    
    # First pass: collect all items
    local all_items=""
    while IFS= read -r line; do
        # Skip section headers and empty lines
        if [[ "$line" =~ ^[[:space:]]*-[[:space:]]+(.*) ]]; then
            local item="${BASH_REMATCH[1]}"
            # Remove [*] marker if present
            item="${item#\[\*\] }"
            if [[ -n "$all_items" ]]; then
                all_items="${all_items}\n• ${item}"
            else
                all_items="• ${item}"
            fi
        fi
    done <<< "$content"
    
    # Second pass: add items until we approach the limit
    while IFS= read -r item; do
        local item_length=${#item}
        local new_total=$((char_count + item_length + 1)) # +1 for newline
        
        if [[ $new_total -le $max_chars ]]; then
            if [[ -n "$result" ]]; then
                result="${result}\n${item}"
                char_count=$((char_count + item_length + 1))
            else
                result="${item}"
                char_count=$item_length
            fi
        else
            # Stop if adding this item would exceed the limit
            break
        fi
    done <<< "$(echo -e "$all_items")"
    
    echo -e "$result"
}

# Main execution
main() {
    if [[ ! -f "$CHANGELOG_FILE" ]]; then
        echo "ERROR: $CHANGELOG_FILE not found" >&2
        exit 1
    fi
    
    # Extract the section content
    section_content=$(extract_section "$VERSION" "$CHANGELOG_FILE")
    
    if [[ -z "$section_content" ]]; then
        echo "ERROR: No content found for version '$VERSION'" >&2
        exit 1
    fi
    
    # Try to extract priority items first
    priority_notes=$(extract_priority_items "$section_content")
    
    if [[ -n "$priority_notes" ]]; then
        # Check if priority items fit within the limit
        char_count=${#priority_notes}
        if [[ $char_count -gt $MAX_CHARS ]]; then
            # Truncate with ellipsis
            truncated="${priority_notes:0:$((MAX_CHARS - 3))}..."
            echo "$truncated"
            echo "WARNING: Priority notes truncated from $char_count to $MAX_CHARS characters" >&2
        else
            echo "$priority_notes"
        fi
    else
        # No priority items found, fall back to extracting all items
        all_notes=$(extract_all_items "$section_content" "$MAX_CHARS")
        if [[ -z "$all_notes" ]]; then
            echo "ERROR: No release notes found" >&2
            exit 1
        fi
        echo "$all_notes"
        echo "INFO: No priority items marked with [*], using all items" >&2
    fi
}

main
