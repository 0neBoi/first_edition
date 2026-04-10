// 简单轮播
const track = document.querySelector(".sh-carousel-track");
const dots = Array.from(document.querySelectorAll(".sh-dot"));

if (track && dots.length) {
  let currentIndex = 0;

  const updateSlide = (index) => {
    const normalized = (index + dots.length) % dots.length;
    currentIndex = normalized;

    track.style.transform = `translateX(-${normalized * 100}%)`;

    dots.forEach((dot, i) => {
      dot.classList.toggle("sh-dot-active", i === normalized);
    });
  };

  dots.forEach((dot) => {
    dot.addEventListener("click", () => {
      const index = Number(dot.dataset.index || "0");
      updateSlide(index);
    });
  });

  setInterval(() => {
    updateSlide(currentIndex + 1);
  }, 6500);
}

// 粒子背景
const canvas = document.getElementById("sh-particle-canvas");
if (canvas) {
  const ctx = canvas.getContext("2d");
  const particles = [];
  const maxParticles = 80;
  const connectDistance = 140;

  const resize = () => {
    canvas.width = window.innerWidth * window.devicePixelRatio;
    canvas.height = window.innerHeight * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  };

  resize();
  window.addEventListener("resize", resize);

  const createParticle = () => {
    const speed = Math.random() * 0.6 + 0.2;
    return {
      x: Math.random() * window.innerWidth,
      y: Math.random() * window.innerHeight,
      vx: (Math.random() - 0.5) * speed,
      vy: (Math.random() - 0.5) * speed,
      size: Math.random() * 2 + 0.4,
      alpha: Math.random() * 0.6 + 0.2,
    };
  };

  for (let i = 0; i < maxParticles; i += 1) {
    particles.push(createParticle());
  }

  let mouse = { x: null, y: null };
  window.addEventListener("mousemove", (e) => {
    mouse = { x: e.clientX, y: e.clientY };
  });

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // 粒子
    particles.forEach((p) => {
      p.x += p.vx;
      p.y += p.vy;

      if (p.x < -50 || p.x > window.innerWidth + 50) p.vx *= -1;
      if (p.y < -50 || p.y > window.innerHeight + 50) p.vy *= -1;

      const gradient = ctx.createRadialGradient(
        p.x,
        p.y,
        0,
        p.x,
        p.y,
        p.size * 6
      );
      gradient.addColorStop(0, `rgba(56, 189, 248, ${p.alpha})`);
      gradient.addColorStop(1, "rgba(15, 23, 42, 0)");

      ctx.fillStyle = gradient;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size * 4, 0, Math.PI * 2);
      ctx.fill();
    });

    // 粒子连线
    for (let i = 0; i < particles.length; i += 1) {
      for (let j = i + 1; j < particles.length; j += 1) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < connectDistance) {
          const alpha = 1 - dist / connectDistance;
          ctx.strokeStyle = `rgba(148, 163, 184, ${alpha * 0.5})`;
          ctx.lineWidth = 0.7;
          ctx.beginPath();
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.stroke();
        }
      }
    }

    // 鼠标交互
    if (mouse.x !== null) {
      particles.forEach((p) => {
        const dx = p.x - mouse.x;
        const dy = p.y - mouse.y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 120) {
          const force = (120 - dist) / 120;
          p.x += (dx / dist) * force * 1.2;
          p.y += (dy / dist) * force * 1.2;
        }
      });
    }

    requestAnimationFrame(draw);
  };

  draw();
}

// 滚动进场动画
const animatedEls = document.querySelectorAll("[data-animate]");
if (animatedEls.length && "IntersectionObserver" in window) {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    },
    {
      threshold: 0.18,
    }
  );

  animatedEls.forEach((el) => observer.observe(el));
}

