function generateRepInputs(setsInput, repsArray = []) {
    const numSets = parseInt(setsInput.value);
    const container = setsInput.closest('.exercise-log-row').querySelector('.reps-container');
    container.innerHTML = ''; 

    for (let i = 0; i < numSets; i++) {
        const repInput = document.createElement('input');
        repInput.type = 'number';
        repInput.className = 'form-control rep-input';
        repInput.placeholder = 'Set ' + (i + 1);
        repInput.style.width = '100px';
        repInput.required = true;
        if (repsArray[i] !== undefined) {
            repInput.value = repsArray[i];
        }
        container.appendChild(repInput);
    }
}

function createAdHocExerciseRow(exerciseId = null, sets = 3, repsArray = []) {
    // Access the global variable 'allExercises' defined in the HTML
    const exerciseOptions = window.allExercises.map(ex => {
        const selected = (exerciseId && ex.id === exerciseId) ? 'selected' : '';
        return `<option value="${ex.id}" ${selected}>${ex.name}</option>`;
    }).join('');

    const row = document.createElement('div');
    row.className = 'mb-3 row align-items-start exercise-log-row adhoc-row';
    row.innerHTML = `
        <div class="col-sm-3">
            <select class="form-select adhoc-exercise-select">
                <option value="" selected disabled>Choose exercise...</option>
                ${exerciseOptions}
            </select>
        </div>
        <div class="col-sm-2">
            <div class="input-group">
                <span class="input-group-text">Sets</span>
                <input type="number" class="form-control sets-input" value="${sets}" required>
            </div>
        </div>
        <div class="col-sm-6">
            <div class="reps-container d-flex flex-wrap gap-2"></div>
        </div>
        <div class="col-sm-1">
            <button type="button" class="btn btn-sm btn-danger remove-row-btn">X</button>
        </div>
    `;
    generateRepInputs(row.querySelector('.sets-input'), repsArray);
    return row;
}

$(document).ready(function() {
    $('.add-adhoc-exercise-btn').click(function() {
        const container = $(this).siblings('.adhoc-exercises-container');
        const newRow = createAdHocExerciseRow();
        container.append(newRow);
    });

    $(document).on('click', '.remove-row-btn', function() {
        // If it's an ad-hoc row, remove it completely
        if ($(this).closest('.exercise-log-row').hasClass('adhoc-row')) {
            $(this).closest('.exercise-log-row').remove();
        } else {
            // If it's a planned row, just hide it and disable inputs so it's not submitted
            const row = $(this).closest('.exercise-log-row');
            row.hide();
            row.find('input, select').prop('disabled', true);
        }
    });

    $('#edit-workout-btn').click(function() {
        $('#view-todays-workout').hide();
        $('#log-new-workout').show();

        if (window.loggedDayId) {
            const collapseElement = $('#collapse-day-' + window.loggedDayId);
            new bootstrap.Collapse(collapseElement, { toggle: true });

            // Reset: Show all planned rows and enable inputs first
            const plannedRows = collapseElement.find('.planned-exercises .exercise-log-row');
            plannedRows.show();
            plannedRows.find('input, select').prop('disabled', false);

            // Reset processed flag on logged exercises
            window.todaysWorkout.forEach(ex => ex.processed = false);

            // Check if it was a Rest Day
            const isRestDay = window.todaysWorkout.some(ex => ex.exerciseName === 'RESTDAY' || ex.exerciseName === 'Restday');

            // 1. Handle Planned Exercises
            plannedRows.each(function() {
                const row = $(this);
                const exerciseId = parseInt(row.find('.personal-exercise-id').val());
                const loggedExercise = window.todaysWorkout.find(ex => ex.personalExerciseId === exerciseId);
                
                if (loggedExercise) {
                    row.find('.sets-input').val(loggedExercise.sets);
                    const repsArray = loggedExercise.reps ? loggedExercise.reps.split(',') : [];
                    generateRepInputs(row.find('.sets-input')[0], repsArray);
                    // Mark as processed to distinguish from ad-hoc
                    loggedExercise.processed = true;
                } else {
                    // If it was a Rest Day, keep planned rows visible (but empty/default) so user can fill them if they want to overwrite Rest Day
                    // If it was NOT a Rest Day, hide the row because it implies the user skipped/deleted it
                    if (!isRestDay) {
                        row.hide();
                        row.find('input, select').prop('disabled', true);
                    } else {
                        // Reset to default state for planned exercise
                        // (Optional: maybe clear values if they were dirty?)
                        // generateRepInputs will use the default value in the HTML
                    }
                }
            });

            // 2. Handle Ad-Hoc Exercises
            const adhocContainer = collapseElement.find('.adhoc-exercises-container');
            adhocContainer.empty(); // Clear any existing ad-hoc rows

            window.todaysWorkout.forEach(loggedExercise => {
                if (!loggedExercise.processed) {
                    // This exercise was not matched with a planned one, so it must be ad-hoc
                    const repsArray = loggedExercise.reps ? loggedExercise.reps.split(',') : [];
                    const newRow = createAdHocExerciseRow(loggedExercise.personalExerciseId, loggedExercise.sets, repsArray);
                    adhocContainer.append(newRow);
                }
            });
        }
    });

    $('#cancel-edit-btn').click(function() {
        $('#log-new-workout').hide();
        $('#view-todays-workout').show();
    });

    $('.collapse').on('shown.bs.collapse', function () {
        $(this).find('.sets-input').each(function() {
            if ($(this).closest('.exercise-log-row').find('.reps-container').is(':empty')) {
                generateRepInputs(this);
            }
        });
    });

    $(document).on('change', '.sets-input', function() {
        generateRepInputs(this);
    });

    $('.workout-log-form').submit(function(e) {
        $(this).find('input[type="hidden"][name^="exercises"]').remove();
        
        $(this).find('.exercise-log-row').each(function(index) {
            const row = $(this);
            
            // Skip hidden rows (deleted planned exercises)
            if (row.is(':hidden')) return;

            let exerciseId;

            if (row.hasClass('adhoc-row')) {
                exerciseId = row.find('.adhoc-exercise-select').val();
            } else {
                exerciseId = row.find('.personal-exercise-id').val();
            }

            if (!exerciseId) return;

            const sets = row.find('.sets-input').val();
            const reps = [];
            row.find('.rep-input').each(function() {
                reps.push($(this).val());
            });
            const repsString = reps.join(',');

            row.append(`<input type="hidden" name="exercises[${index}].personalExerciseId" value="${exerciseId}">`);
            row.append(`<input type="hidden" name="exercises[${index}].sets" value="${sets}">`);
            row.append(`<input type="hidden" name="exercises[${index}].reps" value="${repsString}">`);
        });
        return true;
    });
});
