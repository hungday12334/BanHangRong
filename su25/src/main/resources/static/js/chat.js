class ChatManager {
    constructor() {
        this.currentRoomId = null;
        this.sellerId = 1; // Demo - in real app, get from server
        this.ws = null;
        this.init();
    }

    init() {
        this.connectWebSocket();
        this.bindEvents();
        this.loadChatRooms();
    }

    connectWebSocket() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/chat`;

        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = () => {
            console.log('WebSocket connected');
            if (this.currentRoomId) {
                this.joinRoom(this.currentRoomId);
            }
        };

        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleWebSocketMessage(data);
        };

        this.ws.onclose = () => {
            console.log('WebSocket disconnected');
            // Attempt to reconnect after 3 seconds
            setTimeout(() => this.connectWebSocket(), 3000);
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
    }

    handleWebSocketMessage(data) {
        switch (data.type) {
            case 'new_message':
                this.displayNewMessage(data.data);
                break;
            case 'user_typing':
                this.showTypingIndicator(data.data);
                break;
            case 'error':
                console.error('WebSocket error:', data.message);
                break;
        }
    }

    bindEvents() {
        // Room selection
        document.addEventListener('click', (e) => {
            const roomItem = e.target.closest('.room-item');
            if (roomItem) {
                const roomId = roomItem.dataset.roomid;
                this.selectRoom(roomId);
            }
        });

        // Send message
        document.getElementById('sendMessageBtn')?.addEventListener('click', () => {
            this.sendMessage();
        });

        document.getElementById('messageInput')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });

        // Typing indicator
        document.getElementById('messageInput')?.addEventListener('input', () => {
            this.sendTypingIndicator();
        });
    }

    async selectRoom(roomId) {
        this.currentRoomId = roomId;

        // Update UI
        document.querySelectorAll('.room-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-roomid="${roomId}"]`).classList.add('active');

        // Join WebSocket room
        this.joinRoom(roomId);

        // Load messages
        await this.loadRoomMessages(roomId);

        // Show input
        document.getElementById('chatInput').style.display = 'block';

        // Mark as read
        await this.markAsRead(roomId);
    }

    joinRoom(roomId) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                type: 'join_room',
                roomId: roomId
            }));
        }
    }

    async loadChatRooms() {
        try {
            const response = await fetch(`/seller/chat/api/rooms?sellerId=${this.sellerId}`);
            const rooms = await response.json();
            this.renderChatRooms(rooms);
        } catch (error) {
            console.error('Error loading chat rooms:', error);
        }
    }

    async loadRoomMessages(roomId) {
        try {
            const response = await fetch(`/seller/chat/api/messages/${roomId}`);
            const messages = await response.json();
            this.renderMessages(messages);
        } catch (error) {
            console.error('Error loading messages:', error);
        }
    }

    async sendMessage() {
        const input = document.getElementById('messageInput');
        const content = input.value.trim();

        if (!content || !this.currentRoomId) return;

        try {
            const response = await fetch('/seller/chat/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    roomId: this.currentRoomId,
                    senderId: this.sellerId,
                    content: content,
                    messageType: 'TEXT'
                })
            });

            if (response.ok) {
                input.value = '';
            }
        } catch (error) {
            console.error('Error sending message:', error);
        }
    }

    displayNewMessage(message) {
        if (message.room.roomId === this.currentRoomId) {
            this.renderMessages([message]);
            this.scrollToBottom();
        }
        this.updateUnreadCount();
    }

    renderMessages(messages) {
        const container = document.getElementById('chatMessages');
        container.innerHTML = '';

        messages.forEach(message => {
            const isSent = message.sender.userId === this.sellerId;
            const messageEl = document.createElement('div');
            messageEl.className = `message ${isSent ? 'sent' : 'received'}`;
            messageEl.innerHTML = `
                <div class="message-content">${this.escapeHtml(message.content)}</div>
                <small class="message-time">${new Date(message.createdAt).toLocaleTimeString()}</small>
            `;
            container.appendChild(messageEl);
        });

        this.scrollToBottom();
    }

    renderChatRooms(rooms) {
        const container = document.getElementById('chatRooms');
        container.innerHTML = '';

        rooms.forEach(room => {
            const roomEl = document.createElement('div');
            roomEl.className = 'room-item';
            roomEl.dataset.roomid = room.roomId;
            roomEl.innerHTML = `
                <div class="d-flex align-items-center">
                    <strong>${this.escapeHtml(room.customer.username)}</strong>
                </div>
                ${room.product ? `<small class="text-muted">${this.escapeHtml(room.product.name)}</small>` : ''}
                <small class="text-muted">${new Date(room.updatedAt).toLocaleString()}</small>
            `;
            container.appendChild(roomEl);
        });
    }

    async markAsRead(roomId) {
        try {
            await fetch(`/seller/chat/mark-read?roomId=${roomId}&userId=${this.sellerId}`, {
                method: 'POST'
            });
            this.updateUnreadCount();
        } catch (error) {
            console.error('Error marking as read:', error);
        }
    }

    async updateUnreadCount() {
        try {
            const response = await fetch(`/seller/chat/api/unread-count?userId=${this.sellerId}`);
            const data = await response.json();
            document.getElementById('totalUnread').textContent = `${data.unreadCount} tin nhắn chưa đọc`;

            // Update sidebar badge
            const badge = document.getElementById('unreadChatCount');
            if (data.unreadCount > 0) {
                badge.textContent = data.unreadCount;
                badge.style.display = 'inline';
            } else {
                badge.style.display = 'none';
            }
        } catch (error) {
            console.error('Error updating unread count:', error);
        }
    }

    sendTypingIndicator() {
        if (this.ws && this.ws.readyState === WebSocket.OPEN && this.currentRoomId) {
            this.ws.send(JSON.stringify({
                type: 'typing',
                roomId: this.currentRoomId,
                senderId: this.sellerId
            }));
        }
    }

    showTypingIndicator(data) {
        // Implement typing indicator UI
        console.log('User typing:', data);
    }

    scrollToBottom() {
        const container = document.getElementById('chatMessages');
        container.scrollTop = container.scrollHeight;
    }

    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}

// Initialize chat when page loads
document.addEventListener('DOMContentLoaded', () => {
    new ChatManager();
});

// Thêm vào file chat.js
class AIChatAssistant {
    constructor(chatManager) {
        this.chatManager = chatManager;
        this.isAIEnabled = true;
    }

    analyzeMessage(message) {
        if (!this.isAIEnabled) return null;

        const triggers = {
            'giá|bao nhiêu': 'price_info',
            'tải|download': 'download_info',
            'license|key': 'license_info',
            'cám ơn|thanks': 'thank_you',
            'xin chào|hello|hi': 'greeting'
        };

        for (const [pattern, responseType] of Object.entries(triggers)) {
            if (new RegExp(pattern, 'i').test(message)) {
                return this.generateResponse(responseType);
            }
        }

        return null;
    }

    generateResponse(responseType) {
        const responses = {
            price_info: "Xin chào! Vui lòng kiểm tra giá sản phẩm trên trang chi tiết. Tôi có thể hỗ trợ thêm thông tin nếu bạn cần!",
            download_info: "Sau khi thanh toán thành công, bạn sẽ nhận được link download và license key qua email và trong tài khoản cá nhân.",
            license_info: "License key sẽ được gửi tự động sau khi thanh toán. Mỗi key chỉ active được trên 1 thiết bị.",
            thank_you: "Cám ơn bạn! Nếu có thắc mắc gì thêm, tôi sẵn sàng hỗ trợ 😊",
            greeting: "Xin chào! Tôi là trợ lý ảo. Bạn có thắc mắc gì về sản phẩm không?"
        };

        return responses[responseType] || null;
    }

    suggestQuickReplies() {
        const suggestions = [
            "Sản phẩm này có bản dùng thử không?",
            "Thời hạn license là bao lâu?",
            "Hỗ trợ cài đặt như thế nào?",
            "Có hoàn tiền nếu không hài lòng?"
        ];

        // Hiển thị quick replies UI
        this.showQuickReplies(suggestions);
    }

    showQuickReplies(suggestions) {
        const container = document.getElementById('quickReplies');
        if (!container) return;

        container.innerHTML = suggestions.map(suggestion =>
            `<button class="quick-reply-btn" onclick="chatManager.sendQuickReply('${suggestion}')">
                ${suggestion}
            </button>`
        ).join('');
    }
}

// Update ChatManager class
class ChatManager {
    constructor() {
        // ... existing code ...
        this.aiAssistant = new AIChatAssistant(this);
    }

    // Thêm method mới
    sendQuickReply(text) {
        document.getElementById('messageInput').value = text;
        this.sendMessage();
    }

    // Update handleWebSocketMessage
    handleWebSocketMessage(data) {
        switch (data.type) {
            case 'new_message':
                this.displayNewMessage(data.data);
                // AI analysis for seller
                if (data.data.sender.userId !== this.sellerId) {
                    this.aiAssistant.analyzeCustomerMessage(data.data.content);
                }
                break;
            // ... existing cases ...
        }
    }
}