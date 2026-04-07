/**
 * ═══════════════════════════════════════════════════════
 * LEIKA LUXURY – Hero Slider Controller
 * Horizontal slide with mouse-drag + touch-swipe support
 * ═══════════════════════════════════════════════════════
 */
(function () {
    'use strict';

    var SLIDE_INTERVAL = 6000;   // auto-play ms
    var DRAG_THRESHOLD = 60;     // min px to trigger slide change
    var PARALLAX_FACTOR = 0.25;

    var hero  = document.querySelector('.hero');
    if (!hero) return;

    var track      = hero.querySelector('.hero__track');
    var slides     = hero.querySelectorAll('.hero__slide');
    var dots       = hero.querySelectorAll('.hero__dot');
    var prevBtn    = hero.querySelector('.hero__arrow--prev');
    var nextBtn    = hero.querySelector('.hero__arrow--next');
    var counterCur = hero.querySelector('.hero__counter-current');
    var counterTot = hero.querySelector('.hero__counter-total');

    var current    = 0;
    var total      = slides.length;
    var autoTimer  = null;

    // Drag state
    var isDragging = false;
    var startX     = 0;
    var currentX   = 0;
    var dragDelta  = 0;

    hero.style.setProperty('--slide-duration', SLIDE_INTERVAL + 'ms');

    /* ── Init ──────────────────────────────────────────── */
    function init() {
        if (total === 0) return;
        if (counterTot) counterTot.textContent = pad(total);
        updateSlide(0, true);
        startAuto();
        bindArrowsAndDots();
        bindDrag();
        bindTouch();
        bindKeyboard();
        initParallax();
        document.addEventListener('visibilitychange', function () {
            document.hidden ? stopAuto() : startAuto();
        });
    }

    /* ── Core: go to slide ─────────────────────────────── */
    function updateSlide(index, immediate) {
        // Remove active from old
        slides[current].classList.remove('active');
        dots[current].classList.remove('active');

        current = ((index % total) + total) % total;

        // Position track
        if (immediate) {
            track.classList.add('no-transition');
        } else {
            track.classList.remove('no-transition');
        }
        track.style.transform = 'translateX(-' + (current * 100) + '%)';

        // Force reflow when immediate so transition skip applies
        if (immediate) void track.offsetWidth;
        if (immediate) track.classList.remove('no-transition');

        // Activate new
        slides[current].classList.add('active');
        void dots[current].offsetWidth; // restart progress animation
        dots[current].classList.add('active');

        if (counterCur) counterCur.textContent = pad(current + 1);
    }

    function goNext() { updateSlide(current + 1); }
    function goPrev() { updateSlide(current - 1); }

    /* ── Auto-play ─────────────────────────────────────── */
    function startAuto() { stopAuto(); autoTimer = setInterval(goNext, SLIDE_INTERVAL); }
    function stopAuto()  { if (autoTimer) { clearInterval(autoTimer); autoTimer = null; } }
    function resetAuto() { stopAuto(); startAuto(); }

    /* ── Arrows & Dots ─────────────────────────────────── */
    function bindArrowsAndDots() {
        if (prevBtn) prevBtn.addEventListener('click', function () { goPrev(); resetAuto(); });
        if (nextBtn) nextBtn.addEventListener('click', function () { goNext(); resetAuto(); });
        dots.forEach(function (d, i) {
            d.addEventListener('click', function () {
                if (i !== current) { updateSlide(i); resetAuto(); }
            });
        });
    }

    /* ── Mouse Drag ────────────────────────────────────── */
    function bindDrag() {
        hero.addEventListener('mousedown', onDragStart);
        window.addEventListener('mousemove', onDragMove);
        window.addEventListener('mouseup', onDragEnd);
    }

    function onDragStart(e) {
        // Ignore clicks on buttons/links
        if (e.target.closest('a, button')) return;
        isDragging = true;
        startX = e.clientX;
        currentX = startX;
        dragDelta = 0;
        track.classList.add('no-transition');
        hero.classList.add('is-dragging');
        stopAuto();
        e.preventDefault();
    }

    function onDragMove(e) {
        if (!isDragging) return;
        currentX = e.clientX;
        dragDelta = currentX - startX;
        // Live follow cursor: shift track by drag amount
        var baseOffset = -(current * 100);
        var pxPercent = (dragDelta / hero.offsetWidth) * 100;
        track.style.transform = 'translateX(' + (baseOffset + pxPercent) + '%)';
    }

    function onDragEnd() {
        if (!isDragging) return;
        isDragging = false;
        hero.classList.remove('is-dragging');
        track.classList.remove('no-transition');

        if (Math.abs(dragDelta) > DRAG_THRESHOLD) {
            if (dragDelta < 0) goNext(); else goPrev();
        } else {
            // Snap back
            track.style.transform = 'translateX(-' + (current * 100) + '%)';
        }
        dragDelta = 0;
        resetAuto();
    }

    /* ── Touch Swipe ───────────────────────────────────── */
    function bindTouch() {
        var touchStartX = 0;
        var touchDelta  = 0;
        var touching    = false;

        hero.addEventListener('touchstart', function (e) {
            if (e.target.closest('a, button')) return;
            touching = true;
            touchStartX = e.touches[0].clientX;
            touchDelta = 0;
            track.classList.add('no-transition');
            stopAuto();
        }, { passive: true });

        hero.addEventListener('touchmove', function (e) {
            if (!touching) return;
            touchDelta = e.touches[0].clientX - touchStartX;
            var baseOffset = -(current * 100);
            var pxPercent = (touchDelta / hero.offsetWidth) * 100;
            track.style.transform = 'translateX(' + (baseOffset + pxPercent) + '%)';
        }, { passive: true });

        hero.addEventListener('touchend', function () {
            if (!touching) return;
            touching = false;
            track.classList.remove('no-transition');
            if (Math.abs(touchDelta) > DRAG_THRESHOLD) {
                if (touchDelta < 0) goNext(); else goPrev();
            } else {
                track.style.transform = 'translateX(-' + (current * 100) + '%)';
            }
            touchDelta = 0;
            resetAuto();
        }, { passive: true });
    }

    /* ── Keyboard ──────────────────────────────────────── */
    function bindKeyboard() {
        document.addEventListener('keydown', function (e) {
            var rect = hero.getBoundingClientRect();
            if (rect.bottom < 0 || rect.top > window.innerHeight) return;
            if (e.key === 'ArrowRight') { goNext(); resetAuto(); }
            if (e.key === 'ArrowLeft')  { goPrev(); resetAuto(); }
        });
    }

    /* ── Parallax on Scroll ────────────────────────────── */
    function initParallax() {
        var ticking = false;
        window.addEventListener('scroll', function () {
            if (ticking) return;
            ticking = true;
            requestAnimationFrame(function () {
                var scrollY = window.scrollY;
                var heroH = hero.offsetHeight;
                if (scrollY < heroH) {
                    var offset = scrollY * PARALLAX_FACTOR;
                    var bgs = hero.querySelectorAll('.hero__bg');
                    for (var i = 0; i < bgs.length; i++) {
                        bgs[i].style.transform = 'translateY(' + offset + 'px) scale(1.12)';
                    }
                    var contents = hero.querySelectorAll('.hero__content');
                    var opacity = 1 - (scrollY / (heroH * 0.6));
                    for (var j = 0; j < contents.length; j++) {
                        contents[j].style.opacity = Math.max(0, opacity);
                        contents[j].style.transform = 'translateY(' + (scrollY * 0.15) + 'px)';
                    }
                }
                ticking = false;
            });
        }, { passive: true });
    }

    /* ── Util ──────────────────────────────────────────── */
    function pad(n) { return String(n).padStart(2, '0'); }

    /* ── Launch ────────────────────────────────────────── */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
