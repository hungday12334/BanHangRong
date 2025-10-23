/**
 * Custom Notification System
 * Replaces browser alerts with beautiful popup notifications
 */

// Notification functions
function showNotification(title, message, type = 'success') {
    // Create notification popup if it doesn't exist
    let popup = document.getElementById('notificationPopup');
    if (!popup) {
        createNotificationPopup();
        popup = document.getElementById('notificationPopup');
    }
    
    const icon = document.getElementById('notificationIcon');
    const titleEl = document.getElementById('notificationTitle');
    const textEl = document.getElementById('notificationText');
    
    // Set content
    titleEl.textContent = title;
    textEl.textContent = message;
    
    // Set type and icon
    popup.className = `notification-popup ${type}`;
    if (type === 'success') {
        icon.textContent = '✓';
    } else if (type === 'error') {
        icon.textContent = '✕';
    } else if (type === 'warning') {
        icon.textContent = '⚠';
    } else if (type === 'info') {
        icon.textContent = 'ℹ';
    }
    
    // Show popup
    popup.style.display = 'block';
    popup.style.animation = 'slideInRight 0.3s ease-out';
    
    // Auto hide after 5 seconds
    setTimeout(() => {
        closeNotification();
    }, 5000);
}

function closeNotification() {
    const popup = document.getElementById('notificationPopup');
    if (popup) {
        popup.style.animation = 'slideOutRight 0.3s ease-out';
        setTimeout(() => {
            popup.style.display = 'none';
        }, 300);
    }
}

function createNotificationPopup() {
    // Create notification HTML structure
    const notificationHTML = `
        <div id="notificationPopup" class="notification-popup" style="display: none;">
            <div class="notification-content">
                <div class="notification-icon">
                    <span id="notificationIcon">✓</span>
                </div>
                <div class="notification-message">
                    <h4 id="notificationTitle">Success</h4>
                    <p id="notificationText">Operation completed successfully</p>
                </div>
                <button class="notification-close" onclick="closeNotification()">&times;</button>
            </div>
        </div>
    `;
    
    // Add to body
    document.body.insertAdjacentHTML('beforeend', notificationHTML);
}

// Convenience functions for different notification types
function showSuccess(title, message) {
    showNotification(title, message, 'success');
}

function showError(title, message) {
    showNotification(title, message, 'error');
}

function showWarning(title, message) {
    showNotification(title, message, 'warning');
}

function showInfo(title, message) {
    showNotification(title, message, 'info');
}

// Auto-initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Load CSS if not already loaded
    if (!document.querySelector('link[href*="notification.css"]')) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = '/css/notification.css';
        document.head.appendChild(link);
    }
});
