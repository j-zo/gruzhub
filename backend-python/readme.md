# To start local environment
pdm venv create 3.11
pdm venv activate in-project
pdm install
pdm run uvicorn src.main_app:app --reload

# To lint the project
pdm run pre-commit

# To run tests
pdm run pytest
# To run tests in parallel
pdm run pytest -n auto

# To show coverage
pdm run pytest --cov=src
# To generate coverage report
pdm run pytest --cov=src --cov-report html

# To install library
pdm add library_name

# To run tests
pdm run pytest -s