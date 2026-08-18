import { Component } from 'react';
import ErrorScreen from './ErrorScreen';

// 렌더링 중 예외(오류 화면 코드 로딩 실패 포함)를 잡아 흰 화면 대신 오류 화면을 보여준다.
// 클래스 컴포넌트여야만 하는 이유: React의 에러 바운더리는 아직 훅으로 지원되지 않음.
export default class ErrorBoundary extends Component {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary]', error, info);
  }

  // 재시도는 리듀서/모듈 상태까지 깨끗하게 되돌려야 신뢰할 수 있어서 완전히 새로고침한다.
  // state만 초기화하면 같은 원인으로 즉시 다시 에러가 나는 경우가 많음.
  handleRetry = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    window.location.hash = '#/';
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return <ErrorScreen onRetry={this.handleRetry} onGoHome={this.handleGoHome} />;
    }
    return this.props.children;
  }
}
