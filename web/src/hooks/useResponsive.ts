import { useState, useEffect } from 'react'

/**
 * 自动检测屏幕尺寸，返回响应式断点
 */
export function useResponsive() {
  const [width, setWidth] = useState(typeof window !== 'undefined' ? window.innerWidth : 1200)

  useEffect(() => {
    const handleResize = () => setWidth(window.innerWidth)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return {
    width,
    isMobile: width < 576,
    isTablet: width >= 576 && width < 768,
    isDesktop: width >= 768 && width < 992,
    isWide: width >= 992,
    /** 返回适合当前屏幕的栅格列数 */
    cardCols: width < 576 ? 1 : width < 768 ? 2 : width < 992 ? 3 : width < 1200 ? 4 : 6,
  }
}
