/**
 * Coffee Shot Timer - Theme Switcher
 * Handles light/dark theme switching with localStorage persistence
 */

class ThemeManager {
    constructor() {
        this.themeKey = 'coffee-shot-timer-theme';
        this.htmlElement = document.documentElement;
        this.themeToggle = document.querySelector('.theme-toggle');
        this.metaThemeColor = document.querySelector('meta[name="theme-color"]');
        
        // Theme colors matching the app's Material3 design
        this.themeColors = {
            light: '#B8763D', // Warm Caramel
            dark: '#E6B577'   // Light Caramel
        };
        
        // Initialize theme
        this.init();
    }
    
    init() {
        // Get current theme (might already be set by immediate script)
        let currentTheme = this.getCurrentTheme();
        
        // If no theme is set, determine initial theme
        if (!currentTheme || currentTheme === '') {
            const savedTheme = this.getSavedTheme();
            const systemTheme = this.getSystemTheme();
            currentTheme = savedTheme || systemTheme;
            this.setTheme(currentTheme, false);
        }
        
        // Setup event listeners
        this.setupEventListeners();
        
        // Update theme toggle icon based on current theme
        this.updateToggleIcon();
    }
    
    setupEventListeners() {
        // Theme toggle button
        if (this.themeToggle) {
            this.themeToggle.addEventListener('click', () => {
                this.toggleTheme();
            });
        }
        
        // Listen for system theme changes
        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
                // Only auto-switch if user hasn't manually set a preference
                if (!this.getSavedTheme()) {
                    this.setTheme(e.matches ? 'dark' : 'light', false);
                    this.updateToggleIcon();
                }
            });
        }
        
        // Keyboard shortcut (Ctrl/Cmd + Shift + T)
        document.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'T') {
                e.preventDefault();
                this.toggleTheme();
            }
        });
    }
    
    getSavedTheme() {
        try {
            return localStorage.getItem(this.themeKey);
        } catch (error) {
            console.warn('localStorage not available:', error);
            return null;
        }
    }
    
    getSystemTheme() {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return 'dark';
        }
        return 'light';
    }
    
    getCurrentTheme() {
        return this.htmlElement.getAttribute('data-theme') || 'light';
    }
    
    setTheme(theme, save = true) {
        if (!theme || (theme !== 'light' && theme !== 'dark')) {
            theme = 'light';
        }
        
        // Update HTML attribute
        this.htmlElement.setAttribute('data-theme', theme);
        
        // Update meta theme-color
        if (this.metaThemeColor) {
            this.metaThemeColor.setAttribute('content', this.themeColors[theme]);
        }
        
        // Save to localStorage
        if (save) {
            try {
                localStorage.setItem(this.themeKey, theme);
            } catch (error) {
                console.warn('Could not save theme preference:', error);
            }
        }
        
        // Dispatch custom event for other components to listen to
        window.dispatchEvent(new CustomEvent('themeChanged', {
            detail: { theme }
        }));
    }
    
    toggleTheme() {
        const currentTheme = this.getCurrentTheme();
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';
        
        this.setTheme(newTheme);
        this.updateToggleIcon();
        
        // Add a small haptic feedback if available
        if ('vibrate' in navigator) {
            navigator.vibrate(50);
        }
    }
    
    updateToggleIcon() {
        if (!this.themeToggle) return;
        
        const currentTheme = this.getCurrentTheme();
        const isDark = currentTheme === 'dark';
        
        // Update button title
        this.themeToggle.setAttribute('title', `Switch to ${isDark ? 'light' : 'dark'} mode`);
        this.themeToggle.setAttribute('aria-label', `Switch to ${isDark ? 'light' : 'dark'} mode`);
        
        // Update icon (sun for light, moon for dark)
        const iconSVG = isDark 
            ? `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                 <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
               </svg>`
            : `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                 <circle cx="12" cy="12" r="5"></circle>
                 <line x1="12" y1="1" x2="12" y2="3"></line>
                 <line x1="12" y1="21" x2="12" y2="23"></line>
                 <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                 <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                 <line x1="1" y1="12" x2="3" y2="12"></line>
                 <line x1="21" y1="12" x2="23" y2="12"></line>
                 <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                 <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
               </svg>`;
        
        this.themeToggle.innerHTML = iconSVG;
    }
}

/**
 * Timer Animation Controller
 * Handles the hero timer SVG animation
 */
class TimerAnimation {
    constructor() {
        this.timerElement = document.querySelector('.hero-timer');
        this.isRunning = false;
        this.animationId = null;
        this.startTime = null;
        this.duration = 25000; // 25 seconds in milliseconds
        
        if (this.timerElement) {
            this.init();
        }
    }
    
    init() {
        // Setup intersection observer to start animation when visible
        if ('IntersectionObserver' in window) {
            this.setupIntersectionObserver();
        } else {
            // Fallback for older browsers
            this.startAnimation();
        }
        
        // Respect reduced motion preference
        if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            return; // Skip animation setup
        }
        
        // Click to restart animation
        this.timerElement.addEventListener('click', () => {
            this.restartAnimation();
        });
    }
    
    setupIntersectionObserver() {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting && !this.isRunning) {
                    this.startAnimation();
                } else if (!entry.isIntersecting && this.isRunning) {
                    this.pauseAnimation();
                }
            });
        }, {
            threshold: 0.5 // Start animation when 50% visible
        });
        
        observer.observe(this.timerElement);
    }
    
    startAnimation() {
        if (this.isRunning) return;
        
        this.isRunning = true;
        this.startTime = performance.now();
        this.animate();
    }
    
    pauseAnimation() {
        if (!this.isRunning) return;
        
        this.isRunning = false;
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }
    }
    
    restartAnimation() {
        this.pauseAnimation();
        setTimeout(() => {
            this.startAnimation();
        }, 100);
    }
    
    animate() {
        if (!this.isRunning) return;
        
        const currentTime = performance.now();
        const elapsed = currentTime - this.startTime;
        const progress = Math.min(elapsed / this.duration, 1);
        
        this.updateTimer(progress);
        
        if (progress < 1) {
            this.animationId = requestAnimationFrame(() => this.animate());
        } else {
            // Animation complete, restart after a pause
            setTimeout(() => {
                if (this.isRunning) {
                    this.startTime = performance.now();
                    this.animate();
                }
            }, 2000); // 2 second pause between cycles
        }
    }
    
    updateTimer(progress) {
        // Update time display
        const seconds = Math.floor(progress * 25); // 25 second extraction
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        const timeString = `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
        
        // Find and update time display in SVG
        const textElement = this.timerElement.querySelector('#timer-display');
        if (textElement) {
            textElement.textContent = timeString;
        }
        
        // Update progress circle
        const progressCircle = this.timerElement.querySelector('#progress-circle');
        if (progressCircle) {
            const circumference = 2 * Math.PI * 90; // radius = 90
            const strokeDashoffset = circumference * (1 - progress);
            progressCircle.style.strokeDashoffset = strokeDashoffset;
            
            // Change color based on extraction time
            let color;
            if (progress < 0.6) {
                // Under-extracted (red to orange)
                color = '#D32F2F'; // Error red
            } else if (progress < 0.9) {
                // Optimal zone (green)
                color = '#B8763D'; // Primary color
            } else {
                // Over-extracted (yellow to orange)
                color = '#7BA5A3'; // Secondary color
            }
            
            progressCircle.style.stroke = color;
        }
    }
}

/**
 * Smooth scrolling for anchor links
 */
class SmoothScroll {
    constructor() {
        this.setupSmoothScrolling();
    }
    
    setupSmoothScrolling() {
        // Only add smooth scrolling if user hasn't disabled animations
        if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            return;
        }
        
        document.addEventListener('click', (e) => {
            const link = e.target.closest('a[href^="#"]');
            if (!link) return;
            
            const href = link.getAttribute('href');
            if (href === '#' || !href) return;
            
            const target = document.querySelector(href);
            if (!target) return;
            
            e.preventDefault();
            
            // Calculate offset to account for sticky header
            const headerHeight = document.querySelector('.nav-header')?.offsetHeight || 0;
            const targetPosition = target.offsetTop - headerHeight - 20; // 20px extra padding
            
            window.scrollTo({
                top: Math.max(0, targetPosition),
                behavior: 'smooth'
            });
            
            // Update focus for accessibility
            target.focus({ preventScroll: true });
        });
    }
}

/**
 * Mobile Menu Handler
 * Manages hamburger menu for mobile navigation
 */
class MobileMenu {
    constructor() {
        this.menuToggle = document.querySelector('.menu-toggle');
        this.navMenu = document.querySelector('.nav-menu');
        this.isOpen = false;
        
        if (this.menuToggle && this.navMenu) {
            this.init();
        }
    }
    
    init() {
        // Add click handler to toggle button
        this.menuToggle.addEventListener('click', () => {
            this.toggle();
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', (e) => {
            if (this.isOpen && 
                !this.menuToggle.contains(e.target) && 
                !this.navMenu.contains(e.target)) {
                this.close();
            }
        });
        
        // Close menu on escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
            }
        });
        
        // Close menu when clicking menu links
        this.navMenu.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                this.close();
            });
        });
    }
    
    toggle() {
        this.isOpen ? this.close() : this.open();
    }
    
    open() {
        this.isOpen = true;
        this.menuToggle.setAttribute('aria-expanded', 'true');
        this.navMenu.classList.add('nav-menu-open');
        
        // Update hamburger icon to X
        this.menuToggle.innerHTML = `
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        `;
    }
    
    close() {
        this.isOpen = false;
        this.menuToggle.setAttribute('aria-expanded', 'false');
        this.navMenu.classList.remove('nav-menu-open');
        
        // Update X icon back to hamburger
        this.menuToggle.innerHTML = `
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <line x1="3" y1="12" x2="21" y2="12"></line>
                <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
        `;
    }
}

// Initialize everything when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new ThemeManager();
    new TimerAnimation();
    new SmoothScroll();
    new MobileMenu();
});

// Export for testing purposes
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { ThemeManager, TimerAnimation, SmoothScroll };
}
