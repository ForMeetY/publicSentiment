<script setup>
defineOptions({
  name: 'FooterComponent',
})
</script>

<template>
  <footer class="footer">
    <div class="footer-container">
      <p class="footer-text">© {{ new Date().getFullYear() }} FutureShowX · All Rights Reserved</p>
      <div class="footer-links">
        <a href="#">About FutureShow</a>
        <a href="#">Contact</a>
        <a href="#">Privacy Policy</a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  width: 100%;
  background-color: #1f2937;
  color: #e5e7eb;
  padding: 24px 0;
  margin-top: auto;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.footer-text {
  margin: 0;
  font-size: 14px;
  color: #9ca3af;
}

.footer-links {
  display: flex;
  gap: 20px;
}

.footer-links a {
  color: #d1d5db;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.footer-links a:hover {
  color: #ffffff;
}

/* .footer {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  background-color: #1f2937;
  color: #e5e7eb;
  padding: 20px 0;
} */
</style>
