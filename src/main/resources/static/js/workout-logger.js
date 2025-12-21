function generateSetInputs(setsInput, repsArray = [], weightArray = []) {
    const numSets = parseInt(setsInput.value);
    const container = setsInput.closest('.exercise-log-row').querySelector('.reps-container');
    container.innerHTML = ''; 

    for (let i = 0; i < numSets; i++) {
        const setWrapper = document.createElement('div');
        setWrapper.className = 'd-flex gap-1 mb-1 align-items-center';

        const repInput = document.createElement('input');
        repInput.type = 'number';
        repInput.className = 'form-control rep-input';
        repInput.placeholder = 'Reps';
        repInput.style.width = '80px';
        repInput.required = true;
        if (repsArray[i] !== undefined) {
            repInput.value = repsArray[i];
        }

        const weightInput = document.createElement('input');
        weightInput.type = 'number';
        weightInput.className = 'form-control weight-input';
        weightInput.placeholder = 'kg';
        weightInput.style.width = '80px';
        weightInput.step = '0.5';
        // weightInput.required = true; // Optional: make weight required?
        if (weightArray[i] !== undefined) {
            weightInput.value = weightArray[i];
        }

        const label = document.createElement('span');
        label.className = 'small text-muted';
        label.innerText = (i + 1) + '.';

        setWrapper.appendChild(label);
        setWrapper.appendChild(repInput);
        setWrapper.appendChild(weightInput);
        container.appendChild(setWrapper);
    }
}

function createAdHocExerciseRow(exerciseId = null, sets = 3, repsArray = [], weightArray = []) {
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
            <div class="reps-container d-flex flex-column gap-1"></div>
        </div>
        <div class="col-sm-1">
            <button type="button" class="btn btn-sm btn-danger remove-row-btn">X</button>
        </div>
    `;
    generateSetInputs(row.querySelector('.sets-input'), repsArray, weightArray);
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
            const collapseElement = document.getElementById('collapse-day-' + window.loggedDayId);
            const bsCollapse = new bootstrap.Collapse(collapseElement, { toggle: false });
            bsCollapse.show();

            // Reset: Show all planned rows and enable inputs first
            const plannedRows = $(collapseElement).find('.planned-exercises .exercise-log-row');
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
                    const weightArray = loggedExercise.weight ? loggedExercise.weight.split(',') : [];
                    generateSetInputs(row.find('.sets-input')[0], repsArray, weightArray);
                    // Mark as processed to distinguish from ad-hoc
                    loggedExercise.processed = true;
                } else {
                    if (!isRestDay) {
                        row.hide();
                        row.find('input, select').prop('disabled', true);
                    }
                }
            });

            // 2. Handle Ad-Hoc Exercises
            const adhocContainer = $(collapseElement).find('.adhoc-exercises-container');
            adhocContainer.empty(); // Clear any existing ad-hoc rows

            window.todaysWorkout.forEach(loggedExercise => {
                if (!loggedExercise.processed) {
                    const repsArray = loggedExercise.reps ? loggedExercise.reps.split(',') : [];
                    const weightArray = loggedExercise.weight ? loggedExercise.weight.split(',') : [];
                    const newRow = createAdHocExerciseRow(loggedExercise.personalExerciseId, loggedExercise.sets, repsArray, weightArray);
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
                generateSetInputs(this);
            }
        });
    });

    $(document).on('change', '.sets-input', function() {
        generateSetInputs(this);
    });

    $('.workout-log-form').submit(function(e) {
        $(this).find('input[type="hidden"][name^="exercises"]').remove();
        
        $(this).find('.exercise-log-row').each(function(index) {
            const row = $(this);
            
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
            const weights = [];
            
            row.find('.rep-input').each(function() {
                reps.push($(this).val());
            });
            row.find('.weight-input').each(function() {
                weights.push($(this).val());
            });
            
            const repsString = reps.join(',');
            const weightString = weights.join(',');

            row.append(`<input type="hidden" name="exercises[${index}].personalExerciseId" value="${exerciseId}">`);
            row.append(`<input type="hidden" name="exercises[${index}].sets" value="${sets}">`);
            row.append(`<input type="hidden" name="exercises[${index}].reps" value="${repsString}">`);
            row.append(`<input type="hidden" name="exercises[${index}].weight" value="${weightString}">`);
        });
        return true;
    });
});
