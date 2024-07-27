const form = document.querySelector('form');
form.addEventListener('submit', handleSubmit);

/** @param {Event} event */
function handleSubmit(event) {
    const url = new URL(form.action);
    const formData = new FormData(form);
    
    const fetchOptions = {
        method: form.method,
        body: formData,
    };

    fetch(url, fetchOptions);

    event.preventDefault();
}