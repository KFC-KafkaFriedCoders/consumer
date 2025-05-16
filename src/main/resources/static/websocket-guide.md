# 웹소켓 연결 가이드 (React 애플리케이션용)

이 문서는 React 애플리케이션에서 Spring Boot WebSocket 서버에 연결하는 방법을 안내합니다.

## 기본 정보

- WebSocket 엔드포인트: `/payment-limit-ws`
- SockJS 지원: 예 (WebSocket이 지원되지 않는 환경에서 fallback으로 작동)
- STOMP 프로토콜 사용

## 구독 가능한 토픽

1. **결제 한도 알림 토픽**: `/topic/payment-limit`
   - 결제 한도 관련 이벤트가 발생할 때 메시지 수신

2. **서버 상태 토픽**: `/topic/server-status`
   - 서버 시작/종료 등 상태 변경 시 메시지 수신

## 클라이언트 측 구현 예시 (React)

```bash
# 필요한 의존성 설치
npm install sockjs-client stompjs
```

```jsx
// WebSocketService.js
import SockJS from 'sockjs-client';
import { Client } from 'stompjs';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscribers = {
      paymentLimit: [],
      serverStatus: []
    };
  }

  // 웹소켓 연결
  connect(serverUrl = 'http://localhost:8080/payment-limit-ws') {
    const socket = new SockJS(serverUrl);
    this.stompClient = Client.over(socket);

    return new Promise((resolve, reject) => {
      this.stompClient.connect(
        {},
        frame => {
          console.log('Connected to WebSocket: ', frame);
          this.connected = true;
          
          // 결제 한도 알림 구독
          this.stompClient.subscribe('/topic/payment-limit', message => {
            const data = JSON.parse(message.body);
            this.subscribers.paymentLimit.forEach(callback => callback(data));
          });
          
          // 서버 상태 구독
          this.stompClient.subscribe('/topic/server-status', message => {
            const data = JSON.parse(message.body);
            this.subscribers.serverStatus.forEach(callback => callback(data));
          });
          
          resolve(frame);
        },
        error => {
          console.error('WebSocket 연결 오류: ', error);
          this.connected = false;
          reject(error);
        }
      );
    });
  }

  // 연결 해제
  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect();
      this.connected = false;
      console.log('WebSocket 연결 해제됨');
    }
  }

  // 결제 한도 알림 구독
  subscribeToPaymentLimits(callback) {
    this.subscribers.paymentLimit.push(callback);
    return () => {
      this.subscribers.paymentLimit = this.subscribers.paymentLimit.filter(cb => cb !== callback);
    };
  }

  // 서버 상태 구독
  subscribeToServerStatus(callback) {
    this.subscribers.serverStatus.push(callback);
    return () => {
      this.subscribers.serverStatus = this.subscribers.serverStatus.filter(cb => cb !== callback);
    };
  }

  // 서버에 메시지 전송 (필요한 경우)
  sendMessage(destination, message) {
    if (this.stompClient && this.connected) {
      this.stompClient.send(destination, {}, JSON.stringify(message));
    } else {
      console.error('WebSocket이 연결되어 있지 않습니다.');
    }
  }
}

// 싱글톤 인스턴스 생성
const webSocketService = new WebSocketService();
export default webSocketService;
```

## React 컴포넌트에서 사용 예시

```jsx
// PaymentAlertDashboard.jsx
import React, { useEffect, useState } from 'react';
import webSocketService from './WebSocketService';

const PaymentAlertDashboard = () => {
  const [connected, setConnected] = useState(false);
  const [paymentAlerts, setPaymentAlerts] = useState([]);
  const [serverStatus, setServerStatus] = useState('연결 대기 중...');

  useEffect(() => {
    // 웹소켓 연결
    const connectToWebSocket = async () => {
      try {
        await webSocketService.connect();
        setConnected(true);
      } catch (error) {
        console.error('WebSocket 연결 실패:', error);
        setConnected(false);
      }
    };

    connectToWebSocket();

    // 결제 한도 알림 구독
    const unsubscribePayment = webSocketService.subscribeToPaymentLimits(data => {
      setPaymentAlerts(prev => [data, ...prev].slice(0, 50)); // 최근 50개 알림만 유지
    });

    // 서버 상태 구독
    const unsubscribeStatus = webSocketService.subscribeToServerStatus(data => {
      setServerStatus(data.status);
    });

    // 컴포넌트 언마운트 시 정리
    return () => {
      unsubscribePayment();
      unsubscribeStatus();
      webSocketService.disconnect();
    };
  }, []);

  return (
    <div className="dashboard">
      <div className="status-bar">
        <div className={`status-indicator ${connected ? 'connected' : 'disconnected'}`}>
          {connected ? '연결됨' : '연결 끊김'}
        </div>
        <div className="server-status">{serverStatus}</div>
      </div>

      <h1>결제 한도 알림 대시보드</h1>

      <div className="alerts-container">
        {paymentAlerts.length === 0 ? (
          <div className="no-alerts">알림이 없습니다. 새로운 결제 한도 알림을 기다리는 중...</div>
        ) : (
          paymentAlerts.map((alert, index) => (
            <div key={index} className="alert-card">
              <div className="alert-header">
                <span className="store-name">{alert.store_name}</span>
                <span className="time">{alert.time}</span>
              </div>
              <div className="alert-body">
                <div className="alert-message">{alert.alert_message}</div>
                <div className="alert-details">
                  <div>고객: {alert.user_name} ({alert.user_gender}, {alert.user_age}세)</div>
                  <div>결제금액: {alert.total_price.toLocaleString()}원</div>
                  <div>매장: {alert.store_brand} - {alert.region}</div>
                  <div>주소: {alert.store_address}</div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default PaymentAlertDashboard;
```

## 메시지 형식

### 결제 한도 알림 메시지 (/topic/payment-limit)

```json
{
  "franchise_id": 123,
  "store_brand": "브랜드명",
  "store_id": 456,
  "store_name": "매장명",
  "region": "지역명",
  "store_address": "매장 주소",
  "total_price": 50000,
  "user_id": 789,
  "time": "2025-05-16 14:30:45",
  "user_name": "사용자명",
  "user_gender": "남성",
  "user_age": 30,
  "alert_message": "결제 한도 초과 알림",
  "server_received_time": "2025-05-16 14:30:46",
  "event_type": "payment_limit_alert"
}
```

### 서버 상태 메시지 (/topic/server-status)

```json
{
  "event_type": "server_status",
  "status": "Kafka Consumer 서비스가 시작되었습니다. 토픽: payment_limit",
  "time": "2025-05-16 14:25:30"
}
```

## 문제 해결

1. **연결 오류 발생 시**: 
   - 서버가 실행 중인지 확인하세요.
   - CORS 설정이 올바르게 되어 있는지 확인하세요.
   - 네트워크 방화벽 설정을 확인하세요.

2. **메시지를 수신하지 못하는 경우**:
   - 구독 경로가 올바른지 확인하세요 ('/topic/payment-limit').
   - Kafka 브로커와 토픽이 활성 상태인지 확인하세요.

3. **JSON 파싱 오류**:
   - 메시지 형식이 예상된 형식과 일치하는지 확인하세요.
