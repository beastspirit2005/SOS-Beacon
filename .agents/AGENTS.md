# Project-Specific Rules

## Frontend Verification & Push Workflow
- **Local Testing**: After committing any changes to the frontend (files under `static/`), we must run the local server using `.venv/bin/uvicorn main:app --reload` and verify the frontend on `localhost:8000` (e.g., `/`, `/victim`, `/officer`, `/admin`).
- **Confirmation Requirement**: We must present the local changes and request explicit confirmation from the user.
- **Git Push**: We must not push directly to the remote repository. Only after the user confirms that the changes are correct and approved, we will push the committed changes to the `frontend-AR` branch of `https://github.com/beastspirit2005/SOS-Beacon`.
