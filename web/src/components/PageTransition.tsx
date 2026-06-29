/**
 * PageTransition — 路由切换时的淡入动画
 */
import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'

export default function PageTransition({ children }: { children: React.ReactNode }) {
  const location = useLocation()
  const [display, setDisplay] = useState(children)

  useEffect(() => {
    setDisplay(children)
  }, [location.pathname])

  return (
    <div key={location.pathname} style={{
      animation: 'pageFadeIn 0.3s ease-out',
    }}>
      {display}
    </div>
  )
}
