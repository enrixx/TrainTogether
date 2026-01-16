document.addEventListener('DOMContentLoaded', function() {
    const dateInput = document.getElementById('workoutDateSelect');
    const workoutDetailsEl = document.getElementById('workout-details');
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    // Listen for Tab Change to load list data
    const listTab = document.getElementById('list-tab');
    listTab.addEventListener('shown.bs.tab', function (event) {
        if (currentPage === 0 && document.getElementById('workout-list-container').children.length === 0) {
            loadWorkoutHistory();
        }
    });

    // Calendar Logic
    dateInput.addEventListener('change', function() {
        const dateStr = this.value;
        if (!dateStr) {
            workoutDetailsEl.innerHTML = '<p class="text-muted">No date selected.</p>';
            return;
        }

        workoutDetailsEl.innerHTML = '<p>Loading...</p>';

        fetch(`/workouts/api/by-date?date=${dateStr}`, {
            headers: { 'Accept': 'application/json', [header]: token }
        })
        .then(response => response.json())
        .then(data => {
            if (data && data.length > 0) {
                let html = `<h5>Workout on ${dateStr}</h5>`;
                html += '<ul class="list-group">';
                data.forEach(ex => {
                    let details = '';
                    if (ex.exerciseName !== 'RESTDAY' && ex.exerciseName !== 'Restday') {
                        details = `<br><small class="text-muted">Reps: ${ex.reps} | Weight: ${ex.weight} kg</small>`;
                    }
                    html += `<li class="list-group-item"><strong>${ex.exerciseName}</strong> - ${ex.sets} Sets${details}</li>`;
                });
                html += '</ul>';
                workoutDetailsEl.innerHTML = html;
            } else {
                workoutDetailsEl.innerHTML = '<div class="alert alert-info">No workout logged for this day.</div>';
            }
        })
        .catch(error => {
            console.error('Error fetching workout data:', error);
            workoutDetailsEl.innerHTML = '<p class="text-danger">Could not load workout data.</p>';
        });
    });

    // List Logic
    let currentPage = 0;
    const pageSize = 10;
    const loadMoreBtn = document.getElementById('load-more-btn');
    const listContainer = document.getElementById('workout-list-container');

    loadMoreBtn.addEventListener('click', loadWorkoutHistory);

    function loadWorkoutHistory() {
        loadMoreBtn.disabled = true;
        loadMoreBtn.textContent = 'Loading...';

        fetch(`/workouts/api/history?page=${currentPage}&size=${pageSize}`, {
            headers: { 'Accept': 'application/json', [header]: token }
        })
        .then(response => response.json())
        .then(data => {
            if (data && data.length > 0) {
                data.forEach(day => {
                    const card = createDayCard(day);
                    listContainer.appendChild(card);
                });
                currentPage++;
                loadMoreBtn.disabled = false;
                loadMoreBtn.textContent = 'Load More';
            } else {
                loadMoreBtn.textContent = 'No more data';
                loadMoreBtn.disabled = true;
            }
        })
        .catch(error => {
            console.error('Error loading history:', error);
            loadMoreBtn.textContent = 'Error loading data';
        });
    }

    function createDayCard(day) {
        const card = document.createElement('div');
        card.className = 'card mb-3'; // Added mb-3 for spacing
        
        const statusBadge = day.restDay 
            ? '<span class="badge bg-secondary">Rest Day</span>' 
            : '<span class="badge bg-success">Workout Logged</span>';

        const content = renderDayContent(day);

        card.innerHTML = `
            <div class="card-header d-flex justify-content-between align-items-center bg-body-secondary">
                <strong>${day.date} (${day.weekday})</strong>
                ${statusBadge}
            </div>
            <div class="card-body">
                ${content}
            </div>
        `;
        return card;
    }

    function renderDayContent(day) {
        if (day.restDay) {
            return '<p class="text-muted mb-0">No exercises logged.</p>';
        }
        
        let html = '<ul class="list-group list-group-flush">';
        day.exercises.forEach(ex => {
            html += `<li class="list-group-item">
                <strong>${ex.exerciseName}</strong>: ${ex.sets} Sets
                <small class="text-muted ms-2">(${ex.weight} kg)</small>
            </li>`;
        });
        html += '</ul>';
        return html;
    }
});
