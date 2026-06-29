/**
 * AnimatedNumber — 数字变化时有计数动画效果
 */
import { useEffect, useRef, useState } from 'react'

interface Props {
  value: number
  duration?: number // 动画时长 ms
  style?: React.CSSProperties
}

export default function AnimatedNumber({ value, duration = 600, style }: Props) {
  const [display, setDisplay] = useState(value)
  const prev = useRef(value)

  useEffect(() => {
    const from = prev.current
    const to = value
    if (from === to) return

    const start = performance.now()
    const step = (now: number) => {
      const progress = Math.min((now - start) / duration, 1)
      // easeOutCubic
      const eased = 1 - Math.pow(1 - progress, 3)
      setDisplay(Math.round(from + (to - from) * eased))
      if (progress < 1) requestAnimationFrame(step)
    }
    requestAnimationFrame(step)
    prev.current = to
  }, [value, duration])

  return <span style={style}>{display}</span>
}
