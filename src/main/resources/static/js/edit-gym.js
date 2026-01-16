document.addEventListener('DOMContentLoaded', () => {
    console.log("edit-gym.js loaded");

    const colorPickerElement = document.getElementById('color-picker');
    const hiddenInput = document.getElementById('bannerTextColor');

    if (colorPickerElement && hiddenInput) {
        const initialColor = colorPickerElement.getAttribute('data-initial-color') || '#ffffff';

        const pickr = Pickr.create({
            el: '#color-picker',
            theme: 'classic', // or 'monolith', 'nano'
            default: initialColor,

            swatches: [
                'rgba(244, 67, 54, 1)',
                'rgba(233, 30, 99, 0.95)',
                'rgba(156, 39, 176, 0.9)',
                'rgba(103, 58, 183, 0.85)',
                'rgba(63, 81, 181, 0.8)',
                'rgba(33, 150, 243, 0.75)',
                'rgba(3, 169, 244, 0.7)',
                'rgba(0, 188, 212, 0.7)',
                'rgba(0, 150, 136, 0.75)',
                'rgba(76, 175, 80, 0.8)',
                'rgba(139, 195, 74, 0.85)',
                'rgba(205, 220, 57, 0.9)',
                'rgba(255, 235, 59, 0.95)',
                'rgba(255, 193, 7, 1)'
            ],

            components: {
                // Main components
                preview: true,
                opacity: true,
                hue: true,

                // Input / output Options
                interaction: {
                    hex: true,
                    rgba: true,
                    hsla: true,
                    hsva: true,
                    cmyk: true,
                    input: true,
                    clear: true,
                    save: true
                }
            }
        });

        pickr.on('save', (color, instance) => {
            const hexColor = color ? color.toHEXA().toString() : null;
            hiddenInput.value = hexColor;
            pickr.hide();
        });

        pickr.on('change', (color, source, instance) => {
             const hexColor = color ? color.toHEXA().toString() : null;
             hiddenInput.value = hexColor;
        });
    }

    // --- Worker Management ---
    const workersTab = document.getElementById('workers-tab');
    const gymContainer = document.getElementById('gym-container');
    const gymId = gymContainer ? gymContainer.getAttribute('data-gym-id') : null;
    console.log("Gym ID:", gymId);

    if (workersTab && gymId) {
        // Load initially if tab is already active (e.g. page reload)
        if (workersTab.classList.contains('active')) {
            loadWorkers(gymId);
        }

        workersTab.addEventListener('shown.bs.tab', () => {
            console.log("Workers tab shown");
            loadWorkers(gymId);
        });
    }

    const saveWorkerBtn = document.getElementById('saveWorkerBtn');
    if (saveWorkerBtn) {
        console.log("Save Worker Button found");
        saveWorkerBtn.addEventListener('click', () => {
            console.log("Save Worker Button clicked");
            if (gymId) {
                createWorker(gymId);
            } else {
                alert("Gym ID not found.");
            }
        });
    } else {
        console.error("Save Worker Button NOT found");
    }

    // Worker Delete Delegation
    const workersListContainer = document.getElementById('workers-list-container');
    if (workersListContainer) {
        workersListContainer.addEventListener('click', (event) => {
            if (event.target.closest('.delete-worker-btn')) {
                const btn = event.target.closest('.delete-worker-btn');
                const workerId = btn.getAttribute('data-worker-id');
                if (workerId && gymId) {
                    deleteWorker(workerId, gymId);
                }
            }
        });
    }

    // --- Delete Course Modal ---
    const deleteCourseModal = document.getElementById('deleteCourseModal');
    if (deleteCourseModal) {
        deleteCourseModal.addEventListener('show.bs.modal', event => {
            const button = event.relatedTarget;
            const action = button.getAttribute('data-action');
            const form = deleteCourseModal.querySelector('#deleteCourseForm');
            form.action = action;
        });
    }
});

async function loadWorkers(gymId) {
    console.log("Loading workers for gym:", gymId);
    const container = document.getElementById('workers-list-container');
    container.innerHTML = '<div class="text-center p-4"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div></div>';

    try {
        const response = await fetch(`/gym/api/workers/gym/${gymId}`);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to load workers: ${response.status} ${errorText}`);
        }
        const workers = await response.json();
        console.log("Workers loaded:", workers);

        if (workers.length === 0) {
            container.innerHTML = '<div class="alert alert-light border text-center p-4 text-muted">No workers found.</div>';
            return;
        }

        let html = '<div class="table-responsive"><table class="table table-hover align-middle"><thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Actions</th></tr></thead><tbody>';
        
        workers.forEach(worker => {
            html += `
                <tr>
                    <td>${worker.user.firstName} ${worker.user.lastName}</td>
                    <td>${worker.user.email}</td>
                    <td><span class="badge bg-success">Active</span></td>
                    <td>
                        <button class="btn btn-outline-danger btn-sm delete-worker-btn" data-worker-id="${worker.id}">
                            <i class="bi bi-trash"></i> Delete
                        </button>
                    </td>
                </tr>
            `;
        });

        html += '</tbody></table></div>';
        container.innerHTML = html;

    } catch (error) {
        console.error(error);
        container.innerHTML = `<div class="alert alert-danger">Error loading workers: ${error.message}</div>`;
    }
}

async function createWorker(gymId) {
    const email = document.getElementById('workerEmail').value;
    const password = document.getElementById('workerPassword').value;
    const firstName = document.getElementById('workerFirstName').value;
    const lastName = document.getElementById('workerLastName').value;

    if (!email || !password || !firstName || !lastName) {
        alert('Please fill in all fields');
        return;
    }

    const data = {
        email,
        password,
        firstName,
        lastName,
        gymId: parseInt(gymId)
    };

    // Get CSRF token
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    
    const headers = {
        'Content-Type': 'application/json'
    };

    if (csrfTokenMeta && csrfHeaderMeta) {
        headers[csrfHeaderMeta.getAttribute('content')] = csrfTokenMeta.getAttribute('content');
    } else {
        console.warn("CSRF token not found");
    }

    try {
        const response = await fetch('/gym/api/workers', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(data)
        });

        if (response.ok) {
            // Close modal
            const modalEl = document.getElementById('createWorkerModal');
            let modal = bootstrap.Modal.getInstance(modalEl);
            if (!modal) {
                modal = new bootstrap.Modal(modalEl);
            }
            modal.hide();
            
            // Clear form
            document.getElementById('createWorkerForm').reset();
            
            // Reload list
            loadWorkers(gymId);
        } else {
            const errorData = await response.json();
            console.error("Error creating worker:", errorData);
            alert('Error creating worker: ' + (errorData.message || 'Unknown error'));
        }
    } catch (error) {
        console.error("Fetch error:", error);
        alert('Error creating worker: ' + error.message);
    }
}

async function deleteWorker(workerId, gymId) {
    if (!confirm('Are you sure you want to delete this worker?')) {
        return;
    }

    // Get CSRF token
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    const headers = {};

    if (csrfTokenMeta && csrfHeaderMeta) {
        headers[csrfHeaderMeta.getAttribute('content')] = csrfTokenMeta.getAttribute('content');
    }

    try {
        const response = await fetch(`/gym/api/workers/${workerId}`, {
            method: 'DELETE',
            headers: headers
        });

        if (response.ok) {
            loadWorkers(gymId);
        } else {
            const errorText = await response.text();
            alert('Error deleting worker: ' + errorText);
        }
    } catch (error) {
        console.error(error);
        alert('Error deleting worker: ' + error.message);
    }
}
