#!/bin/bash
# Extract priority changes from CHANGELOG.md for Play Store "What's New"
# Usage: ./scripts/extract-whatsnew.sh [version|unreleased] [max_chars]
#
# Priority changes are marked with [*] at the beginning of the bullet point in CHANGELOG.md
# Example: - [*] Major new feature that users will love
#
# Output is capped at max_chars INCLUDING the trailing newline (Play rejects
# longer files), and only whole bullets are kept - never a mid-sentence cut.

set -euo pipefail

VERSION="${1:-unreleased}"
MAX_CHARS="${2:-500}"
CHANGELOG_FILE="${3:-CHANGELOG.md}"

# Function to extract section content
extract_section() {
    local version="$1"
    local file="$2"

    if [[ "$version" == "unreleased" ]]; then
        # Extract from [Unreleased] section (everything up to the next heading)
        awk '
            BEGIN{p=0}
            /^## \[/{
                if (p==1) exit
                if ($0 ~ /^## \[Unreleased\]/) p=1
                next
            }
            p==1{print}
        ' "$file"
    else
        # Extract from specific version section
        awk -v v="$version" '
            BEGIN{p=0}
            /^## \[/{
                if (p==1) exit
                if ($0 ~ "^## \\[(" v ")\\]") p=1
                next
            }
            p==1{print}
        ' "$file"
    fi
}

# Function to extract priority items only, one per line
extract_priority_items() {
    local content="$1"

    while IFS= read -r line; do
        # Priority items start with - [*]; strip the marker, keep the text.
        if [[ "$line" =~ ^[[:space:]]*-[[:space:]]*\[\*\][[:space:]]*(.*) ]]; then
            printf '%s\n' "• ${BASH_REMATCH[1]}"
        fi
    done <<< "$content"
}

# Function to extract all items (fallback if no priority items), one per line
extract_all_items() {
    local content="$1"

    while IFS= read -r line; do
        if [[ "$line" =~ ^[[:space:]]*-[[:space:]]+(.*) ]]; then
            local item="${BASH_REMATCH[1]}"
            item="${item#\[\*\] }"
            printf '%s\n' "• ${item}"
        fi
    done <<< "$content"
}

# Print whole lines from stdin until the next line would push the output
# (including the trailing newline) past max_chars. A single line longer than
# the limit is hard-truncated with an ellipsis instead of dropped.
emit_within_limit() {
    local max_chars="$1"
    local result=""
    local char_count=0

    while IFS= read -r line; do
        local line_length=${#line}
        # +1 for the newline between lines, +1 for the trailing newline
        local new_total=$((char_count == 0 ? line_length : char_count + 1 + line_length))
        if ((new_total + 1 > max_chars)); then
            if [[ -z "$result" ]]; then
                # Nothing emitted yet and this line alone is too long
                printf '%s\n' "${line:0:$((max_chars - 4))}..."
            fi
            break
        fi
        if [[ -n "$result" ]]; then
            result+=$'\n'
            ((char_count += 1))
        fi
        result+="$line"
        ((char_count += line_length))
    done

    [[ -n "$result" ]] && printf '%s\n' "$result"
}

# Main execution
main() {
    if [[ ! -f "$CHANGELOG_FILE" ]]; then
        echo "ERROR: $CHANGELOG_FILE not found." >&2
        exit 1
    fi

    # Extract the section content
    section_content=$(extract_section "$VERSION" "$CHANGELOG_FILE")

    if [[ -z "$section_content" ]]; then
        echo "ERROR: No content found for version '$VERSION'" >&2
        exit 1
    fi

    # Try to extract priority items first, fall back to all items
    notes=$(extract_priority_items "$section_content")
    if [[ -z "$notes" ]]; then
        notes=$(extract_all_items "$section_content")
        echo "INFO: No priority items marked with [*], using all items" >&2
    fi

    if [[ -z "$notes" ]]; then
        echo "ERROR: No release notes found" >&2
        exit 1
    fi

    emit_within_limit "$MAX_CHARS" <<< "$notes"
}

main
